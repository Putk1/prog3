package com.o3.server;

import com.sun.net.httpserver.*;

import java.io.*;
import javax.net.ssl.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.stream.Collectors;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.Executors;

public class Server implements HttpHandler {

    private Server() {
    }
    
    @Override
    public void handle(HttpExchange t) throws IOException {
        if ("POST".equals(t.getRequestMethod())) {
            handlePostRequest(t);
        } else if ("GET".equals(t.getRequestMethod())) {
            handleGetRequest(t);
        } else if ("PUT".equals(t.getRequestMethod())) {
            handlePutRequest(t);
        } else {
            handleResponse(t);
        }
    }

    private void handleResponse(HttpExchange httpExchange) throws IOException {
        String responseString = "Not supported";
        byte[] bytes = responseString.getBytes("UTF-8");
        httpExchange.sendResponseHeaders(400, bytes.length);

        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(responseString.getBytes());
        outputStream.flush();
        outputStream.close();
    }


    private void handleGetRequest(HttpExchange httpExchange) throws IOException {
        try {
            List<ObservationRecord> records = MessageDatabase.getInstance().getMessages();

            if (records.isEmpty()) {
                httpExchange.sendResponseHeaders(204, -1);
                return;
            }

            JSONArray responseArray = new JSONArray();
            for (ObservationRecord record : records) {
                responseArray.put(record.toJSON());
            }

            String responseString = responseArray.toString();
            byte[] bytes = responseString.getBytes("UTF-8");

            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            httpExchange.sendResponseHeaders(200, bytes.length);
            
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(responseString.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            httpExchange.sendResponseHeaders(500, -1);
        }
    }

    private void handlePostRequest(HttpExchange httpExchange) throws IOException {
        try {
            long epochMilli = java.time.Instant.now().toEpochMilli();

            String username = httpExchange.getPrincipal().getUsername();
            User user = MessageDatabase.getInstance().getUser(username);
            String nickname = user.getNickname();

            InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String text = br.lines().collect(Collectors.joining("\n"));
            
            JSONObject json = new JSONObject(text);

            if (!json.has("orbital_elements") && !json.has("state_vector")) {
                httpExchange.sendResponseHeaders(400, -1);
                httpExchange.getResponseBody().close();
                return;
            }

            new ObservationRecord(json, nickname, 0, "");

            MessageDatabase.getInstance().storeMessage(nickname, epochMilli, text);

            httpExchange.sendResponseHeaders(201, -1);

        } catch (Exception e) {
            String response = "Invalid request format";
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            httpExchange.sendResponseHeaders(400, responseBytes.length);
            httpExchange.getResponseBody().write(responseBytes);
        } finally {
            httpExchange.getResponseBody().close();
        }

    }

    private void handlePutRequest(HttpExchange httpExchange) throws IOException {
        try {
            String query = httpExchange.getRequestURI().getQuery();
            if (query == null || !query.startsWith("id=")) {
                httpExchange.sendResponseHeaders(400, -1);
                return;
            }

            long id;
            try {
                id = Long.parseLong(query.substring(3));
            } catch (NumberFormatException e) {
                httpExchange.sendResponseHeaders(400, -1);
                return;
            }

            // User info
            String username = httpExchange.getPrincipal().getUsername();
            User user = MessageDatabase.getInstance().getUser(username);
            String nickname = user.getNickname();

            InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String text = br.lines().collect(Collectors.joining("\n"));

            JSONObject json = new JSONObject(text);

            if (!json.has("orbital_elements") && !json.has("state_vector")) {
                httpExchange.sendResponseHeaders(400, -1);
                return;
            }

            new ObservationRecord(json, nickname, id, "");

            ObservationRecord existingRecord = MessageDatabase.getInstance().getMessageById(id);
            if (existingRecord == null) {
                httpExchange.sendResponseHeaders(404, -1);
                return;
            }

            if (!existingRecord.getRecordOwner().equals(nickname)) {
                httpExchange.sendResponseHeaders(403, -1);
                return;
            }

            MessageDatabase.getInstance().updateMessage(id, text);

            httpExchange.sendResponseHeaders(200, -1);
        } catch (Exception e) {
            String response = "Invalid format for request";
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            httpExchange.sendResponseHeaders(400, responseBytes.length);
            httpExchange.getResponseBody().write(responseBytes);
        } finally {
            httpExchange.getResponseBody().close();
        }
    }

    private static SSLContext myServerSSLContext(String keystorePath, String password) throws Exception{
        char[] passphrase = password.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keystorePath), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ssl;
    }

    public static void main(String[] args) throws Exception {
        String dbPath = System.getenv("DATABASE_PATH");
        
        if (dbPath == null || dbPath.isEmpty()) {
            System.err.println("DATABASE_PATH not set");
            return;
        }

        MessageDatabase.getInstance().open(dbPath);
        
        try {
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001),0);

            SSLContext sslContext = myServerSSLContext(args[0], args[1]);
            server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
                public void configure (HttpsParameters params) {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

            UserAuthenticator auth = new UserAuthenticator("datarecord");

            HttpContext context = server.createContext("/datarecord", new Server());
            context.setAuthenticator(auth);

            HttpContext collectionsContext = server.createContext("/collections", new CollectionsHandler());
            collectionsContext.setAuthenticator(auth);

            server.createContext("/registration", new RegistrationHandler(auth));

            server.setExecutor(Executors.newCachedThreadPool());

            server.start();
            System.out.println("Server running on port 8001");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server");
                server.stop(0);
                try {
                    MessageDatabase.getInstance().closeDatabase();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

        } catch (FileNotFoundException e) {
            System.out.println("Certificate not found!");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
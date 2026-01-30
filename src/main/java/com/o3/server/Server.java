package com.o3.server;

import com.sun.net.httpserver.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.stream.Collectors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class Server implements HttpHandler {
    
    StringBuilder textDump = new StringBuilder();

    private Server() {
    }
    
    @Override
    public void handle(HttpExchange t) throws IOException {
        if ("POST".equals(t.getRequestMethod())) {
            handlePostRequest(t);
        } else if ("GET".equals(t.getRequestMethod())) { 
            handleGetRequest(t);
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
        String responseString = "No messages";
        if (textDump.toString().length() != 0) {
            responseString = textDump.toString();
        }

        byte[] bytes = responseString.getBytes("UTF-8");
        httpExchange.sendResponseHeaders(200, bytes.length);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(responseString.getBytes());
        outputStream.flush();
        outputStream.close();
    }

    private void handlePostRequest(HttpExchange httpExchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(),StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        
        String text = br.lines().collect(Collectors.joining("\n"));
        textDump.append(text);
        
        br.close();
        isr.close();
        
        httpExchange.getResponseHeaders();
        httpExchange.sendResponseHeaders(200, -1);

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

            server.createContext("/registration", new RegistrationHandler(auth));

            server.setExecutor(null);
            server.start();
            System.out.println("Server running on port 8001");

        } catch (FileNotFoundException e) {
            // Certificate file not found!
            System.out.println("Certificate not found!");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
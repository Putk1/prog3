package com.o3.server;

import com.sun.net.httpserver.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

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

    public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(8001),0);
    server.createContext("/datarecord", new Server());
    server.setExecutor(null);
    server.start();
    System.out.println("Server running on port 8001");
    }
}
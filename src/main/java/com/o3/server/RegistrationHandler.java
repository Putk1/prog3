package com.o3.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class RegistrationHandler implements HttpHandler {
    
    private UserAuthenticator auth;

    public RegistrationHandler(UserAuthenticator auth) {
        this.auth = auth;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
            handlePostRequest(t);
        } else {
            String response = "Not supported";
            t.sendResponseHeaders(400, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private void handlePostRequest(HttpExchange t) throws IOException {
        InputStreamReader isr = new InputStreamReader(t.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String body = br.lines().collect(Collectors.joining("\n"));
        br.close();

        String[] parts = body.split(":");

        if (parts.length != 2) {
            String response = "Invalid format. Use username:password";
            t.sendResponseHeaders(400, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        String username = parts[0];
        String password = parts[1];

        boolean success = auth.addUser(username, password);

        if (success) {
            String response = "User registration successful";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            String response = "User already registered";
            t.sendResponseHeaders(403, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}

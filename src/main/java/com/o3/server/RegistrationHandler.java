package com.o3.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;
import org.json.JSONException;

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
        try {
            InputStreamReader isr = new InputStreamReader(t.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String body = br.lines().collect(Collectors.joining("\n"));
            br.close();

            JSONObject json = new JSONObject(body);

            User newUser = new User(
                json.getString("username"),
                json.getString("password"),
                json.getString("email"),
                json.getString("nickname")
            );

            boolean success = auth.addUser(newUser);

            if (success) {
                String response = "User registration successful";
                t.getResponseHeaders().set("Content-Type", "application/json");
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                String response = "User already registered";
                t.getResponseHeaders().set("Content-Type", "application/json");
                t.sendResponseHeaders(403, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }

        } catch (JSONException e) {
            String response = "Invalid JSON format";
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(400, response.length());
            try (OutputStream os = t.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}

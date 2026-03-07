package com.o3.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class CollectionsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        String path = t.getRequestURI().getPath();
        String method = t.getRequestMethod();

        try {
            if ("GET".equalsIgnoreCase(method)) {
                if (path.equals("/collections") || path.equals("/collections/")) {
                    JSONArray ids = MessageDatabase.getInstance().getAllCollectionIds();
                    sendJsonResponse(t, 200, ids.toString());
                } else {
                    String[] parts = path.split("/");
                    if (parts.length >= 3) {
                        long collectionId = Long.parseLong(parts[2]);
                        List<ObservationRecord> records = MessageDatabase.getInstance().getCollectionMessages(collectionId);
                        JSONArray responseArray = new JSONArray();
                        for (ObservationRecord record : records) {
                            responseArray.put(record.toJSON());
                        }
                        sendJsonResponse(t, 200, responseArray.toString());
                    } else {
                        sendJsonResponse(t, 404, "{\"error\": \"Not Found\"}");
                    }
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(t);
                String trimmedBody = body.trim();
                
                if (path.equals("/collections/create") || path.equals("/collections")) {

                    String name = "Unnamed Collection";
                    JSONArray messageIds = null;

                    if (!trimmedBody.isEmpty()) {
                        if (trimmedBody.startsWith("[")) {
                            messageIds = new JSONArray(trimmedBody); 
                        } else if (trimmedBody.startsWith("{")) {
                            JSONObject json = new JSONObject(trimmedBody);
                            name = json.optString("name", "Unnamed Collection");
                            if (json.has("message_ids")) {
                                messageIds = json.getJSONArray("message_ids");
                            }
                        }
                    }

                    long newCollectionId = MessageDatabase.getInstance().createCollection(name);
                    
                    if (messageIds != null && messageIds.length() > 0) {
                        MessageDatabase.getInstance().addMessagesToCollection(newCollectionId, messageIds);
                    }

                    JSONObject response = new JSONObject();
                    response.put("created_collection_id", newCollectionId);
                    sendJsonResponse(t, 200, response.toString());
                
                } else if (path.equals("/collections/add")) {
                    JSONObject json = new JSONObject(trimmedBody);
                    long collectionId = json.getLong("collection_id");
                    JSONArray messageIds = json.getJSONArray("message_ids");
                    
                    MessageDatabase.getInstance().addMessagesToCollection(collectionId, messageIds);
                    sendJsonResponse(t, 200, "{}");
                } else {
                    sendJsonResponse(t, 404, "{\"error\": \"Path not found\"}");
                }
            } else {
                sendJsonResponse(t, 400, "{\"error\": \"Method not supported\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(t, 400, "{\"error\": \"Bad Request\"}");
        }
    }

    private String readBody(HttpExchange t) throws IOException {
        InputStreamReader isr = new InputStreamReader(t.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        return br.lines().collect(Collectors.joining("\n"));
    }

    private void sendJsonResponse(HttpExchange t, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = t.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
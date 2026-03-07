package com.o3.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class CollectionsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
        String path = t.getRequestURI().getPath();
        String method = t.getRequestMethod();

        try {
            if ("GET".equalsIgnoreCase(method)) {
                if (path.equals("/collections")) {
                    JSONArray ids = MessageDatabase.getInstance().getAllCollectionIds();
                    sendJsonResponse(t, 200, ids.toString());
                } else {
                    String idStr = path.substring(path.lastIndexOf('/') + 1);
                    long collectionId = Long.parseLong(idStr);
                    
                    List<ObservationRecord> records = MessageDatabase.getInstance().getCollectionMessages(collectionId);
                    JSONArray responseArray = new JSONArray();
                    for (ObservationRecord record : records) {
                        responseArray.put(record.toJSON());
                    }
                    sendJsonResponse(t, 200, responseArray.toString());
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(t);
                
                if (path.equals("/collections")) {

                    String trimmedBody = body.trim();
                    JSONObject json;

                    if (trimmedBody.isEmpty()) {
                        json = new JSONObject(); 
                    } else {
                        json = new JSONObject(body); 
                    }

                    String name = json.optString("name", "Unnamed Collection");
                    long newCollectionId = MessageDatabase.getInstance().createCollection(name);
                    
                    if (json.has("message_ids")) {
                        JSONArray messageIds = json.getJSONArray("message_ids");
                        MessageDatabase.getInstance().addMessagesToCollection(newCollectionId, messageIds);
                    }

                JSONObject response = new JSONObject();
                response.put("created_collection_id", newCollectionId);
                sendJsonResponse(t, 200, response.toString());
                
                } else if (path.equals("/collections/add")) {
                    JSONObject json = new JSONObject(body);
                    long collectionId = json.getLong("collection_id");
                    JSONArray messageIds = json.getJSONArray("message_ids");
                    
                    MessageDatabase.getInstance().addMessagesToCollection(collectionId, messageIds);
                    sendJsonResponse(t, 200, "{}");
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
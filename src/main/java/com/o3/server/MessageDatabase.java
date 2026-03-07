package com.o3.server;

import java.sql.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.apache.commons.codec.digest.Crypt;
import java.security.SecureRandom;
public class MessageDatabase {
    private static MessageDatabase instance = null;
    private Connection connection = null;

    private SecureRandom secureRandom = new SecureRandom();

    private MessageDatabase() {}

    public static synchronized MessageDatabase getInstance() {
        if (instance == null) {
            instance = new MessageDatabase();
        }
        return instance;
    }

    public void open(String dbPath) throws SQLException {
        String url = "jdbc:sqlite:" + dbPath;
        boolean exists = new File(dbPath).exists();
        connection = DriverManager.getConnection(url);
        if (!exists) initializeDatabase();
    }

    public void closeDatabase() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    private void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // User table
            stmt.executeUpdate("CREATE TABLE users (" +
                "username TEXT PRIMARY KEY, " +
                "password TEXT NOT NULL, " +
                "email TEXT, " +
                "nickname TEXT)");
            
            // Messages table
            stmt.executeUpdate("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "owner TEXT, " +
                "time INTEGER, " +
                "payload TEXT)");
            
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS collections (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS collection_messages (" +
                "collection_id INTEGER, " +
                "message_id INTEGER, " +
                "FOREIGN KEY(collection_id) REFERENCES collections(id), " +
                "FOREIGN KEY(message_id) REFERENCES messages(id))");
        }
    }

    public synchronized long createCollection(String name) throws SQLException {
        String sql = "INSERT INTO collections (name) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to create a collection");
    }

    public synchronized void addMessagesToCollection(long collectionId, JSONArray messageIds) throws SQLException {
        String sql = "INSERT INTO collection_messages (collection_id, message_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < messageIds.length(); i++) {
                pstmt.setLong(1, collectionId);
                pstmt.setLong(2, messageIds.getLong(i));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public synchronized JSONArray getAllCollectionIds() throws SQLException {
        JSONArray ids = new JSONArray();
        String sql = "SELECT id FROM collections";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ids.put(rs.getLong("id"));
            }
        }
        return ids;
    }

    public synchronized List<ObservationRecord> getCollectionMessages(long collectionId) throws SQLException {
        List<ObservationRecord> records = new ArrayList<>();
        String sql = "SELECT m.* FROM messages m " +
                     "JOIN collection_messages cm ON m.id = cm.message_id " +
                     "WHERE cm.collection_id = ?";
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, collectionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject payloadJson = new JSONObject(rs.getString("payload"));
                    long epochMilli = rs.getLong("time");
                    ZonedDateTime utcTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.of("UTC"));
                    records.add(new ObservationRecord(
                        payloadJson,
                        rs.getString("owner"),
                        rs.getLong("id"),
                        utcTime.format(formatter)
                    ));
                }
            }
        }
        return records;
    }

    public synchronized ObservationRecord getMessageById(long id) throws SQLException {
        String sql = "SELECT * FROM messages WHERE id = ?";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JSONObject payloadJson = new JSONObject(rs.getString("payload"));
                    long epochMilli = rs.getLong("time");
                    ZonedDateTime utcTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.of("UTC"));
                    String isoTime = utcTime.format(formatter);
                    
                    return new ObservationRecord(payloadJson, rs.getString("owner"), rs.getLong("id"), isoTime);
                }
            }
        }
        return null;
    }

    public synchronized void updateMessage(long id, String payload, long time) throws SQLException {
        String sql = "UPDATE messages SET payload = ?, time = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, payload);
            pstmt.setLong(2, time);
            pstmt.setLong(3, id);
            pstmt.executeUpdate();
        }
    }

    public synchronized boolean registerUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, email, nickname) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());

            // Hash the password and store the hashed password
            String hashedPassword = Crypt.crypt(user.getPassword());
            pstmt.setString(2, hashedPassword);

            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getNickname());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    public synchronized User getUser(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getString("username"), rs.getString("password"),
                                rs.getString("email"), rs.getString("nickname"));
            }
        }
        return null;
    }

    public synchronized void storeMessage(String ownerNickname, long time, String payload) throws SQLException {
        String sql = "INSERT INTO messages (owner, time, payload) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerNickname);
            pstmt.setLong(2, time);
            pstmt.setString(3, payload);
            pstmt.executeUpdate();
        }
    }

    public synchronized List<ObservationRecord> getMessages() throws SQLException {
        List<ObservationRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM messages";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                JSONObject payloadJson = new JSONObject(rs.getString("payload"));

                long epochMilli = rs.getLong("time");
                ZonedDateTime utcTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.of("UTC"));
                String isoTime = utcTime.format(formatter);

                records.add(new ObservationRecord(
                    payloadJson,
                    rs.getString("owner"),
                    rs.getLong("id"),
                    isoTime
                ));
            }
        }
        return records;
    }
}

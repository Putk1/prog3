package com.o3.server;

import java.sql.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class MessageDatabase {
    private static MessageDatabase instance = null;
    private Connection connection = null;

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
        }
    }

    public boolean registerUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, email, nickname) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getNickname());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    public User getUser(String username) throws SQLException {
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

    public void storeMessage(String ownerNickname, long time, String payload) throws SQLException {
        String sql = "INSERT INTO messages (owner, time, payload) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerNickname);
            pstmt.setLong(2, time);
            pstmt.setString(3, payload);
            pstmt.executeUpdate();
        }
    }

    public List<ObservationRecord> getMessages() throws SQLException {
        List<ObservationRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM messages";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                JSONObject payloadJson = new JSONObject(rs.getString("payload"));
                String isoTime = java.time.Instant.ofEpochMilli(rs.getLong("time")).toString();

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

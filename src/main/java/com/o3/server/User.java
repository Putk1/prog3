package com.o3.server;

import org.json.JSONObject;

public class User {
    private String username;
    private String password;
    private String email;
    private String nickname;

    public User(String username, String password, String email, String nickname) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.nickname = nickname;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("username", username);
        obj.put("password", password);
        obj.put("email", email);
        obj.put("nickname", nickname);
        return obj;
    }
}

package com.o3.server;

import java.util.Hashtable;
import java.util.Map;

public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {
    
    private Map<String, User> users = new Hashtable<>();
    
    public UserAuthenticator(String realm) {
        super("datarecord");
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password)) {
            return true;
        }
        return false;
    }

    public boolean addUser(User user) {
        if (users.containsKey(user.getUsername())) {
            return false;
        }
        users.put(user.getUsername(), user);
        return true;
    }
}

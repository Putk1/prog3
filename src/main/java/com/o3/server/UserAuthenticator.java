package com.o3.server;

import java.util.Hashtable;
import java.util.Map;

public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {
    
    private Map<String,String> users = null;
    
    public UserAuthenticator(String realm) {
        super("datarecord");
        users = new Hashtable<String,String>();
        users.put("dummy", "passwd"); 
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        String storedPassword = users.get(username);

        if (storedPassword != null && storedPassword.equals(password)) {
            return true;
        }

        return false;
    }

    public boolean addUser(String userName, String password) {
        if (users.containsKey(userName)) {
            return false;
        }
        users.put(userName, password);
        return true;
    }
}

package com.o3.server;

import org.apache.commons.codec.digest.Crypt;
public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {
    
    public UserAuthenticator(String realm) {
        super(realm);
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        try {
            User user = MessageDatabase.getInstance().getUser(username);
            if (user != null) {
                String storedHashedPassword = user.getPassword();

                // Check the password against the hashed password in DB
                if (storedHashedPassword.equals(Crypt.crypt(password, storedHashedPassword))) {
                    return true;
                }

            }
            return false;
        } catch (Exception e) { return false; }
    }

    public boolean addUser(User user) {
        try {
            return MessageDatabase.getInstance().registerUser(user);
        } catch (Exception e) { return false; }
    }
}

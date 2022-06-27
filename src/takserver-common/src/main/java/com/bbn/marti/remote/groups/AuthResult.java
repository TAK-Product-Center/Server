package com.bbn.marti.remote.groups;

import java.io.Serializable;

/*
 * 
 * Simple wrapper class that just contains an AuthStatus and the associated User
 * 
 */
public class AuthResult implements Serializable {
    
    private static final long serialVersionUID = 3667656662272198924L;
    
    private AuthStatus authStatus;
    private User user;

    public AuthResult(AuthStatus authStatus, User user) {
        this.authStatus = authStatus;
        this.user = user;
    }

    public AuthStatus getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(AuthStatus authStatus) {
        this.authStatus = authStatus;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AuthResult [authStatus=");
        builder.append(authStatus);
        builder.append(", user=");
        builder.append(user);
        builder.append("]");
        return builder.toString();
    }
}

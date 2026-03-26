package com.estudy.app.model.response;

public class AuthResponse {
    private String token;
    private boolean authenticated;

    public String getToken() { return token; }
    public boolean isAuthenticated() { return authenticated; }
}
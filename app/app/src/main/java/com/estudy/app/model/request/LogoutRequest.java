package com.estudy.app.model.request;

public class LogoutRequest {
    private String token;

    public LogoutRequest(String token) {
        this.token = token;
    }
}
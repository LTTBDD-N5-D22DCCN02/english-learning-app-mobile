package com.estudy.app.model.request;

public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phone;
    private String dob;

    public RegisterRequest(String username, String password, String email,
                           String fullName, String phone, String dob) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.dob = dob;
    }
}
package com.estudy.app.model.response;

public class ClassMemberResponse {
    private String id;
    private String userId;
    private String username;
    private String fullName;
    private String role;    // "LEADER" | "MEMBER" | "ADMIN"
    private String status;  // "APPROVED" | "PENDING" | "REJECTED"
    private String createdAt;

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
package com.estudy.app.model.response;

public class CommentResponse {
    private String id;
    private String content;
    private String username;
    private String fullName;
    private String createdAt;

    public String getId() { return id; }
    public String getContent() { return content; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getCreatedAt() { return createdAt; }
}
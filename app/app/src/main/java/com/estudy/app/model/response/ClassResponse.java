package com.estudy.app.model.response;

public class ClassResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private String privacy;   // "PUBLIC" | "PRIVATE"
    private String createdAt;
    private int memberCount;
    private String myRole;    // "LEADER" | "MEMBER" | "ADMIN"

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPrivacy() { return privacy; }
    public String getCreatedAt() { return createdAt; }
    public int getMemberCount() { return memberCount; }
    public String getMyRole() { return myRole; }
}
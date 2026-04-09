package com.estudy.app.model.request;

public class UpdateMemberRoleRequest {
    private String role; // "MEMBER" | "ADMIN"

    public UpdateMemberRoleRequest(String role) { this.role = role; }
    public String getRole() { return role; }
}
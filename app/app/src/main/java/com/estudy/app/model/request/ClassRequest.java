package com.estudy.app.model.request;

public class ClassRequest {
    private String name;
    private String description;
    private String privacy; // "PUBLIC" | "PRIVATE"

    public ClassRequest(String name, String description, String privacy) {
        this.name = name;
        this.description = description;
        this.privacy = privacy;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPrivacy() { return privacy; }
}
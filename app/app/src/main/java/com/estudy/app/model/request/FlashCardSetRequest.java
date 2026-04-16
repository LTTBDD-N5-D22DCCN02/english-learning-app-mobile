package com.estudy.app.model.request;

public class FlashCardSetRequest {
    private String name;
    private String description;
    private String privacy;

    public FlashCardSetRequest(String name, String description, String privacy) {
        this.name = name;
        this.description = description;
        this.privacy = privacy;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPrivacy() { return privacy; }
}
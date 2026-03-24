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
}
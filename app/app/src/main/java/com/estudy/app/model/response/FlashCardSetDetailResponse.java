package com.estudy.app.model.response;

import java.util.List;

public class FlashCardSetDetailResponse {
    private String id;
    private String name;
    private String description;
    private String privacy;
    private String createdAt;
    private List<FlashCardResponse> flashCards;
    private List<CommentResponse> comments;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPrivacy() { return privacy; }
    public List<FlashCardResponse> getFlashCards() { return flashCards; }
    public List<CommentResponse> getComments() { return comments; }
}
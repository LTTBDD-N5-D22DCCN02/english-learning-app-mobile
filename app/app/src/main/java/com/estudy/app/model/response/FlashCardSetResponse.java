package com.estudy.app.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FlashCardSetResponse {

    private String id;
    private String name;
    private String description;
    private String privacy;
    private String createdAt;
    private int cardCount;

    // === THÊM 2 FIELD NÀY (quan trọng) ===
    @SerializedName("ownerId")
    private String ownerId;      // Dùng String để dễ so sánh với currentUserId

    @SerializedName("ownerName")
    private String ownerName;

    // ==================== GETTERS ====================

    private List<FlashCardResponse> flashCards;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPrivacy() { return privacy; }
    public List<FlashCardResponse> getFlashCards() { return flashCards; }
    public String getCreatedAt() { return createdAt; }

    public String getOwnerId() {
        return ownerId;
    }
    public int    getCardCount()   { return cardCount; }
    public String getOwnerName() {
        return ownerName;
    }

    // ==================== SETTERS (nếu cần) ====================

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrivacy(String privacy) { this.privacy = privacy; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}
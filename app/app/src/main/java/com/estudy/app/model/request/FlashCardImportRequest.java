package com.estudy.app.model.request;

import com.google.gson.annotations.SerializedName;

public class FlashCardImportRequest {
    @SerializedName("content") public String content;

    public FlashCardImportRequest(String content) {
        this.content = content;
    }
}
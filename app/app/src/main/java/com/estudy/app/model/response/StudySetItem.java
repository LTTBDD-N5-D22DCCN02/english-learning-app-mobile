package com.estudy.app.model.response;

import java.util.List;

public class StudySetItem {
    private String setId;
    private String setName;
    private int wordCount;          // số từ due/new trong bộ này
    private int totalWords;         // tổng số từ trong bộ
    private int rememberedCount;    // số từ đã nhớ (dùng cho progress bar)
    private String lastReviewedAt;  // "yesterday", "3 days ago", "Never studied"
    private List<String> previewTerms; // preview 2-3 từ đầu

    public String getSetId() { return setId; }
    public String getSetName() { return setName; }
    public int getWordCount() { return wordCount; }
    public int getTotalWords() { return totalWords; }
    public int getRememberedCount() { return rememberedCount; }
    public String getLastReviewedAt() { return lastReviewedAt; }
    public List<String> getPreviewTerms() { return previewTerms; }
}

package com.estudy.backend.dto.response;

import java.util.List;

public class StudySetItemResponse {
    private String setId;
    private String setName;
    private int wordCount;
    private int totalWords;
    private int rememberedCount;
    private String lastReviewedAt;
    private List<String> previewTerms;

    public StudySetItemResponse(String setId, String setName, int wordCount,
                                int totalWords, int rememberedCount, String lastReviewedAt,
                                List<String> previewTerms) {
        this.setId = setId;
        this.setName = setName;
        this.wordCount = wordCount;
        this.totalWords = totalWords;
        this.rememberedCount = rememberedCount;
        this.lastReviewedAt = lastReviewedAt;
        this.previewTerms = previewTerms;
    }

    public String getSetId() { return setId; }
    public String getSetName() { return setName; }
    public int getWordCount() { return wordCount; }
    public int getTotalWords() { return totalWords; }
    public int getRememberedCount() { return rememberedCount; }
    public String getLastReviewedAt() { return lastReviewedAt; }
    public List<String> getPreviewTerms() { return previewTerms; }
}
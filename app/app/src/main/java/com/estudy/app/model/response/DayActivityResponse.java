package com.estudy.app.model.response;

public class DayActivityResponse {
    private String date;          // "2025-01-15"
    private int    wordCount;
    private double accuracyPercent;

    public String getDate()            { return date; }
    public int    getWordCount()       { return wordCount; }
    public double getAccuracyPercent() { return accuracyPercent; }
}

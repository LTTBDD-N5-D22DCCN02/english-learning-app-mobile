package com.estudy.app.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SessionResultResponse {
    private String sessionId;
    private String mode;
    private int correctCount;
    private int totalQuestions;
    private int durationSeconds;
    private int currentStreak;
    @SerializedName("isNewRecord")
    private boolean newRecord;
    private List<String> wrongTerms;

    public String  getSessionId()      { return sessionId; }
    public String  getMode()           { return mode; }
    public int     getCorrectCount()   { return correctCount; }
    public int     getTotalQuestions() { return totalQuestions; }
    public int     getDurationSeconds(){ return durationSeconds; }
    public int     getCurrentStreak()  { return currentStreak; }
    public boolean isNewRecord()       { return newRecord; }
    public List<String> getWrongTerms(){ return wrongTerms; }
}

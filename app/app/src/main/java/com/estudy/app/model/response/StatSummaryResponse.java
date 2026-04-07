package com.estudy.app.model.response;

import com.google.gson.annotations.SerializedName;

public class StatSummaryResponse {
    private int    wordsLearned;
    private int    wordsMastered;
    private long   totalAnswers;
    private long   wrongAnswers;
    private double accuracyPercent;
    private int    currentStreak;
    private int    longestStreak;
    @SerializedName("isNewRecord")
    private boolean newRecord;

    public int     getWordsLearned()   { return wordsLearned; }
    public int     getWordsMastered()  { return wordsMastered; }
    public long    getTotalAnswers()   { return totalAnswers; }
    public long    getWrongAnswers()   { return wrongAnswers; }
    public double  getAccuracyPercent(){ return accuracyPercent; }
    public int     getCurrentStreak()  { return currentStreak; }
    public int     getLongestStreak()  { return longestStreak; }
    public boolean isNewRecord()       { return newRecord; }
}

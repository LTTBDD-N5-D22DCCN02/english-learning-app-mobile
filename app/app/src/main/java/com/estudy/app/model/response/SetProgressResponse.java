package com.estudy.app.model.response;

public class SetProgressResponse {
    private String setId;
    private String setName;
    private int    totalWords;
    private int    rememberedCount;
    private int    notYetCount;
    private int    notStudiedCount;
    private double percentage;

    public String getSetId()          { return setId; }
    public String getSetName()        { return setName; }
    public int    getTotalWords()     { return totalWords; }
    public int    getRememberedCount(){ return rememberedCount; }
    public int    getNotYetCount()    { return notYetCount; }
    public int    getNotStudiedCount(){ return notStudiedCount; }
    public double getPercentage()     { return percentage; }
}

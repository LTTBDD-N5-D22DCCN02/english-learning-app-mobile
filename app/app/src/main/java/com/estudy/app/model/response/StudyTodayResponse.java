package com.estudy.app.model.response;

import java.util.List;

public class StudyTodayResponse {
    private int totalDue;   // tổng từ cần ôn
    private int totalNew;   // tổng từ mới
    private List<StudySetItem> dueSets;  // bộ có từ cần ôn
    private List<StudySetItem> newSets;  // bộ có từ mới

    public int getTotalDue() { return totalDue; }
    public int getTotalNew() { return totalNew; }
    public List<StudySetItem> getDueSets() { return dueSets; }
    public List<StudySetItem> getNewSets() { return newSets; }
}

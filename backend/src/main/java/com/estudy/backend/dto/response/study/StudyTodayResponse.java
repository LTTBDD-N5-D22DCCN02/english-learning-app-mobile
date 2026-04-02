package com.estudy.backend.dto.response;

import java.util.List;

public class StudyTodayResponse {
    private int totalDue;
    private int totalNew;
    private List<StudySetItemResponse> dueSets;
    private List<StudySetItemResponse> newSets;

    public StudyTodayResponse(int totalDue, int totalNew,
                              List<StudySetItemResponse> dueSets,
                              List<StudySetItemResponse> newSets) {
        this.totalDue = totalDue;
        this.totalNew = totalNew;
        this.dueSets = dueSets;
        this.newSets = newSets;
    }

    public int getTotalDue() { return totalDue; }
    public int getTotalNew() { return totalNew; }
    public List<StudySetItemResponse> getDueSets() { return dueSets; }
    public List<StudySetItemResponse> getNewSets() { return newSets; }
}
package com.estudy.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatSummaryResponse {

    // UC-STAT-01: Vocabulary Progress
    int wordsLearned;   // tổng thẻ có StudyRecord
    int wordsMastered;  // remembered = true

    // UC-STAT-02: Answer Accuracy
    long totalAnswers;
    long wrongAnswers;
    double accuracyPercent; // (total - wrong) / total * 100

    // UC-STAT-03: Study Streak
    int currentStreak;
    int longestStreak;
    boolean isNewRecord;
}
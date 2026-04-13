package com.estudy.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionResultResponse {

    String sessionId;
    String mode;
    int correctCount;
    int totalQuestions;
    int durationSeconds;

    /** Streak sau phiên học */
    int currentStreak;
    @JsonProperty("isNewRecord")
    boolean newRecord;

    /** Danh sách từ trả lời sai để hiển thị màn hình kết quả */
    List<String> wrongTerms;
}
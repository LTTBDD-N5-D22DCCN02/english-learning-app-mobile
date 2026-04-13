package com.estudy.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartSessionResponse {

    String sessionId;
    String setId;
    String setName;
    String mode;

    /** Danh sách thẻ cần học trong phiên này */
    List<SessionCardResponse> cards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionCardResponse {
        String flashcardId;
        String term;
        String definition;
        String ipa;
        String audioUrl;
        String image;
        String example;

        /** Với word_quiz: 3 đáp án nhiễu + 1 đúng */
        List<String> distractors;
    }
}
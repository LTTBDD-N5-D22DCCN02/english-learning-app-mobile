package com.estudy.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnswerRequest {

    @NotNull UUID sessionId;
    @NotNull UUID flashcardId;

    /**
     * Flashcard mode: remembered = true/false
     * Quiz/Spelling/Match: correct = true/false
     * Gửi cả 2 field, backend sẽ dùng đúng field theo mode của session.
     */
    Boolean remembered; // flashcard mode
    Boolean correct;    // word_quiz / match / spelling
}
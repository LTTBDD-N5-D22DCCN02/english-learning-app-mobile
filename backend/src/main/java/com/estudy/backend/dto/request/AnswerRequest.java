package com.estudy.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerRequest {
    private String flashcardId;
    private String setId;
    private String mode;    // "flashcard" | "spelling" | "match" | "word_quiz"
    private Boolean correct;
}

package com.estudy.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerRequest {
    private String flashcardId;
    private String sessionId;
    private Boolean remembered;  // cho flashcard mode
    private Boolean correct;     // cho quiz / word_quiz mode
}

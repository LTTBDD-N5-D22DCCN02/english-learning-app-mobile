package com.estudy.app.model.request;

public class AnswerRequest {
    private String flashcardId;
    private String sessionId;
    private Boolean remembered;  // cho flashcard mode
    private Boolean correct;     // cho quiz / word_quiz mode

    public AnswerRequest(String flashcardId, String sessionId, Boolean remembered, Boolean correct) {
        this.flashcardId = flashcardId;
        this.sessionId   = sessionId;
        this.remembered  = remembered;
        this.correct     = correct;
    }

    public String  getFlashcardId() { return flashcardId; }
    public String  getSessionId()   { return sessionId; }
    public Boolean getRemembered()  { return remembered; }
    public Boolean getCorrect()     { return correct; }
}

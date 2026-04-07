package com.estudy.app.model.request;

public class AnswerRequest {
    private String flashcardId;
    private String setId;
    private String mode;    // "flashcard" | "spelling" | "match" | "word_quiz"
    private Boolean correct;

    public AnswerRequest(String flashcardId, String setId, String mode, boolean correct) {
        this.flashcardId = flashcardId;
        this.setId = setId;
        this.mode = mode;
        this.correct = correct;
    }

    public String getFlashcardId() { return flashcardId; }
    public String getSetId() { return setId; }
    public String getMode() { return mode; }
    public Boolean getCorrect() { return correct; }
}

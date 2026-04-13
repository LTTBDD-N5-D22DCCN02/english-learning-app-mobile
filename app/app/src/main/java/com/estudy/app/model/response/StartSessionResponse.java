package com.estudy.app.model.response;

import java.util.List;

public class StartSessionResponse {
    private String sessionId;
    private String setId;
    private String setName;
    private String mode;
    private List<SessionCardResponse> cards;

    public String getSessionId() { return sessionId; }
    public String getSetId()     { return setId; }
    public String getSetName()   { return setName; }
    public String getMode()      { return mode; }
    public List<SessionCardResponse> getCards() { return cards; }

    public static class SessionCardResponse {
        private String flashcardId;
        private String term;
        private String definition;
        private String ipa;
        private String audioUrl;
        private String image;
        private String example;
        private List<String> distractors;

        public String getFlashcardId() { return flashcardId; }
        public String getTerm()        { return term; }
        public String getDefinition()  { return definition; }
        public String getIpa()         { return ipa; }
        public String getAudioUrl()    { return audioUrl; }
        public String getImage()       { return image; }
        public String getExample()     { return example; }
        public List<String> getDistractors() { return distractors; }
    }
}

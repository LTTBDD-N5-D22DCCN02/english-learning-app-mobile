package com.estudy.app.model.response;

public class FlashCardResponse {
    private String id;
    private String term;
    private String definition;
    private String image;
    private String ipa;
    private String example;

    public String getId() { return id; }
    public String getTerm() { return term; }
    public String getDefinition() { return definition; }
    public String getImage() { return image; }
    public String getIpa() { return ipa; }
    public String getExample() { return example; }

    public void setId(String id) { this.id = id; }
    public void setTerm(String term) { this.term = term; }
    public void setDefinition(String definition) { this.definition = definition; }
    public void setImage(String image) { this.image = image; }
    public void setIpa(String ipa) { this.ipa = ipa; }
    public void setExample(String example) { this.example = example; }

}
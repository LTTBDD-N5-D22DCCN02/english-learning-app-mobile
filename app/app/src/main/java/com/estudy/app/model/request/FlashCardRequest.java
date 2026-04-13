package com.estudy.app.model.request;

import com.google.gson.annotations.SerializedName;

public class FlashCardRequest {
    @SerializedName("term")       public String term;
    @SerializedName("definition") public String definition;
    @SerializedName("ipa")        public String ipa;
    @SerializedName("example")    public String example;
    @SerializedName("image")      public String image;

    public FlashCardRequest(String term, String definition, String ipa, String example, String image) {
        this.term = term;
        this.definition = definition;
        this.ipa = ipa;
        this.example = example;
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
package com.estudy.app.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SuggestResponse {
    @SerializedName("term")     public String term;
    @SerializedName("ipa")      public String ipa;
    @SerializedName("meanings") public List<Meaning> meanings;

    public static class Meaning {
        @SerializedName("partOfSpeech") public String partOfSpeech;
        @SerializedName("definition")   public String definition;
        @SerializedName("example")      public String example;
        @SerializedName("synonyms")     public List<String> synonyms;
    }
}
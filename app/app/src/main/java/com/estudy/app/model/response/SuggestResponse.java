package com.estudy.app.model.response;

import com.google.gson.annotations.SerializedName;

public class SuggestResponse {
    @SerializedName("term")       public String term;
    @SerializedName("definition") public String definition;
    @SerializedName("ipa")        public String ipa;
    @SerializedName("example")    public String example;

    public String getTerm() { return term; }
    public String getDefinition() { return definition; }
    public String getIpa() { return ipa; }
    public String getExample() { return example; }
}
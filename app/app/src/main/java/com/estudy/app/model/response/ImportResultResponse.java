package com.estudy.app.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ImportResultResponse {
    @SerializedName("successCount")  public int successCount;
    @SerializedName("failedCount")   public int failedCount;
    @SerializedName("errors")        public List<String> errors;
    @SerializedName("importedCards") public List<FlashCardResponse> importedCards;
}
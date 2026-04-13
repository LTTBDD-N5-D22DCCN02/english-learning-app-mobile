package com.estudy.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportResultResponse {
    private int successCount;
    private int failedCount;
    private List<String> errors;
    private List<FlashCardResponse> importedCards;
}
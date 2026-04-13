package com.estudy.backend.controller;

import com.estudy.backend.dto.request.FlashCardImportRequest;
import com.estudy.backend.dto.request.FlashCardRequest;
import com.estudy.backend.dto.response.ApiResponse;
import com.estudy.backend.dto.response.FlashCardResponse;
import com.estudy.backend.dto.response.ImportResultResponse;
import com.estudy.backend.dto.response.SuggestResponse;
import com.estudy.backend.service.FlashCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
// ĐÃ XÓA @RequestMapping("/flashcards") ĐỂ KHÔNG BỊ NỐI CHUỖI SAI
public class FlashCardController {

    private final FlashCardService flashcardService;

    // Khớp với @GET("flashcards") trên Android
    @GetMapping("/flashcards")
    public ApiResponse<List<FlashCardResponse>> getFlashcards(
            @RequestParam("deck_id") UUID deckId) {
        return ApiResponse.<List<FlashCardResponse>>builder()
                .result(flashcardService.getFlashCardsBySet(deckId))
                .build();
    }

    // Khớp với @POST("flashcard-sets/{setId}/flashcards") trên Android
    @PostMapping("/flashcard-sets/{setId}/flashcards")
    public ApiResponse<FlashCardResponse> createFlashcard(
            @PathVariable UUID setId,
            @Valid @RequestBody FlashCardRequest request) {
        return ApiResponse.<FlashCardResponse>builder()
                .result(flashcardService.createFlashCard(setId, request))
                .build();
    }

    // Khớp với @POST("flashcard-sets/{setId}/flashcards/import") trên Android
    @PostMapping("/flashcard-sets/{setId}/flashcards/import")
    public ApiResponse<ImportResultResponse> importFlashcards(
            @PathVariable UUID setId,
            @Valid @RequestBody FlashCardImportRequest request) {
        return ApiResponse.<ImportResultResponse>builder()
                .result(flashcardService.importFlashcards(setId, request))
                .build();
    }

    // Khớp với @PUT("flashcards/{id}") trên Android
    @PutMapping("/flashcards/{id}")
    public ApiResponse<FlashCardResponse> updateFlashcard(
            @PathVariable UUID id,
            @Valid @RequestBody FlashCardRequest request) {
        return ApiResponse.<FlashCardResponse>builder()
                .result(flashcardService.updateFlashCard(id, request))
                .build();
    }

    // Khớp với @DELETE("flashcards/{id}") trên Android
    @DeleteMapping("/flashcards/{id}")
    public ApiResponse<Void> deleteFlashcard(@PathVariable UUID id) {
        flashcardService.deleteFlashCard(id);
        return ApiResponse.<Void>builder()
                .result(null)
                .build();
    }

    // Khớp với @GET("flashcards/suggest") trên Android
    @GetMapping("/flashcards/suggest")
    public ApiResponse<SuggestResponse> suggest(@RequestParam String term) {
        return ApiResponse.<SuggestResponse>builder()
                .result(flashcardService.suggestForTerm(term))
                .build();
    }
}
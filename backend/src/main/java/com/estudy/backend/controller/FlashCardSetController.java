package com.estudy.backend.controller;

import com.estudy.backend.dto.request.FlashCardSetRequest;
import com.estudy.backend.dto.response.ApiResponse;
import com.estudy.backend.dto.response.FlashCardSetDetailResponse;
import com.estudy.backend.dto.response.FlashCardSetResponse;
import com.estudy.backend.service.FlashCardSetService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/flashcard-sets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlashCardSetController {

    FlashCardSetService flashCardSetService;

    // 1. Tạo bộ flashcard
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<FlashCardSetResponse> create(@RequestBody @Valid FlashCardSetRequest request) {
        return ApiResponse.<FlashCardSetResponse>builder()
                .result(flashCardSetService.create(request))
                .build();
    }

    // 2. Chỉnh sửa bộ flashcard
    @PutMapping("/{id}")
    ApiResponse<FlashCardSetResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid FlashCardSetRequest request) {
        return ApiResponse.<FlashCardSetResponse>builder()
                .result(flashCardSetService.update(id, request))
                .build();
    }

    // 3. Xoá mềm bộ flashcard
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) {
        flashCardSetService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Flashcard set deleted successfully")
                .build();
    }

    // 4. Danh sách bộ flashcard của user đang đăng nhập
    @GetMapping("/my")
    ApiResponse<List<FlashCardSetResponse>> getMyFlashCardSets() {
        return ApiResponse.<List<FlashCardSetResponse>>builder()
                .result(flashCardSetService.getMyFlashCardSets())
                .build();
    }

    // 5. Chi tiết 1 bộ flashcard
    @GetMapping("/{id}")
    ApiResponse<FlashCardSetDetailResponse> getDetail(@PathVariable UUID id) {
        return ApiResponse.<FlashCardSetDetailResponse>builder()
                .result(flashCardSetService.getDetail(id))
                .build();
    }
}
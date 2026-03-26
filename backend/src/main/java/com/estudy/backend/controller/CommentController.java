package com.estudy.backend.controller;

import com.estudy.backend.dto.request.CommentRequest;
import com.estudy.backend.dto.response.ApiResponse;
import com.estudy.backend.dto.response.CommentResponse;
import com.estudy.backend.service.CommentService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/flashcard-sets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentController {

    CommentService commentService;

    // 1. Thêm comment vào bộ flashcard
    @PostMapping("/{flashCardSetId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CommentResponse> create(
            @PathVariable UUID flashCardSetId,
            @RequestBody @Valid CommentRequest request) {
        return ApiResponse.<CommentResponse>builder()
                .result(commentService.create(flashCardSetId, request))
                .build();
    }

    // 2. Xoá comment
    @DeleteMapping("/comments/{commentId}")
    ApiResponse<Void> delete(@PathVariable UUID commentId) {
        commentService.delete(commentId);
        return ApiResponse.<Void>builder()
                .message("Comment deleted successfully")
                .build();
    }
}
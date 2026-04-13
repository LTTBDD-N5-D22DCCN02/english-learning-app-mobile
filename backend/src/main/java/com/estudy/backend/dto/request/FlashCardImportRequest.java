package com.estudy.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlashCardImportRequest {

    @NotBlank(message = "Content is required")
    private String content;
}
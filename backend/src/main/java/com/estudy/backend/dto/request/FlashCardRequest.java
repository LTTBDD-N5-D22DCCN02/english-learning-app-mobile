package com.estudy.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FlashCardRequest {

    @NotBlank(message = "Term is required")
    @Size(max = 500, message = "Term must not exceed 500 characters")
    private String term;

    private String definition;
    private String ipa;
    private String example;
    private String image;
}
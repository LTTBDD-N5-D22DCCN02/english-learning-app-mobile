package com.estudy.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartSessionRequest {

    @NotNull(message = "setId must not be null")
    UUID setId;

    /** flashcard | word_quiz | match | spelling */
    @NotBlank(message = "mode must not be blank")
    String mode;
}
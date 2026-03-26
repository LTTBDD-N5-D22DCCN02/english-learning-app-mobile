package com.estudy.backend.dto.request;

import com.estudy.backend.enums.Privacy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlashCardSetRequest {

    @NotBlank(message = "FLASHCARD_SET_NAME_REQUIRED")
    @Size(max = 255, message = "FLASHCARD_SET_NAME_INVALID")
    String name;

    String description;

    @NotNull(message = "FLASHCARD_SET_PRIVACY_REQUIRED")
    Privacy privacy;
}
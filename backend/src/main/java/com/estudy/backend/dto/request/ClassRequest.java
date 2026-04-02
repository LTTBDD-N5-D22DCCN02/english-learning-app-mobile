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
public class ClassRequest {

    @NotBlank(message = "CLASS_NAME_REQUIRED")
    @Size(max = 255, message = "Class name must not exceed 255 characters")
    String name;

    String description;

    @NotNull(message = "Privacy is required")
    Privacy privacy;
}
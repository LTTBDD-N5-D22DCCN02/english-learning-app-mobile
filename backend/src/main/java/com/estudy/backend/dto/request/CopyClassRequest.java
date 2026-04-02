package com.estudy.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CopyClassRequest {

    @NotBlank(message = "CLASS_NAME_REQUIRED")
    @Size(max = 255, message = "Class name must not exceed 255 characters")
    String name;
}
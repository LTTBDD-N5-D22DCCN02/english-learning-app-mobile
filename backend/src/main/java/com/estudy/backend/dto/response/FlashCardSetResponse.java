package com.estudy.backend.dto.response;

import com.estudy.backend.enums.Privacy;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlashCardSetResponse {
    UUID id;
    String name;
    String description;
    Privacy privacy;
    LocalDateTime createdAt;
}
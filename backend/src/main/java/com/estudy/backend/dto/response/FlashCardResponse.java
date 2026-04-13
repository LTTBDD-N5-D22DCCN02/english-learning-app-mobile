package com.estudy.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlashCardResponse {
    UUID id;
    String term;
    String definition;
    String image;
    String ipa;
    String example;
    LocalDateTime createdAt;
    private Long flashcardSetId;
}
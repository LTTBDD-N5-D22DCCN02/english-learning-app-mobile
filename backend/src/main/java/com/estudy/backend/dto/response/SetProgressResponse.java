package com.estudy.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SetProgressResponse {

    String setId;
    String setName;
    int totalWords;
    int rememberedCount;
    int notYetCount;
    int notStudiedCount;
    double percentage; // rememberedCount / totalWords * 100
}
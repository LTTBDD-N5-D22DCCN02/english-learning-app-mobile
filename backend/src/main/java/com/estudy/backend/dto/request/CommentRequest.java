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
public class CommentRequest {

    @NotBlank(message = "COMMENT_CONTENT_REQUIRED")
    @Size(max = 1000, message = "COMMENT_CONTENT_INVALID")
    String content;
}
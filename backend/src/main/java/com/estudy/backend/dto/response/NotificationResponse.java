package com.estudy.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    String id;
    String type;        // "join_request" | "vocab_reminder" | "new_set" | "new_comment"
    String title;
    String content;
    boolean isRead;
    String createdAt;
    NotificationMetadata metadata;
}

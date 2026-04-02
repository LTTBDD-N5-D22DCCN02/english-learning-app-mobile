package com.estudy.backend.dto.response;

import com.estudy.backend.enums.ClassMemberRole;
import com.estudy.backend.enums.ClassMemberStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassMemberResponse {
    UUID id;
    UUID userId;
    String username;
    String fullName;
    ClassMemberRole role;
    ClassMemberStatus status;
    LocalDateTime createdAt;
}
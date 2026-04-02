package com.estudy.backend.dto.response;

import com.estudy.backend.enums.ClassMemberRole;
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
public class ClassResponse {
    UUID id;
    String code;
    String name;
    String description;
    Privacy privacy;
    LocalDateTime createAt;
    int memberCount;
    ClassMemberRole myRole;
}
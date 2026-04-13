package com.estudy.backend.dto.request;

import com.estudy.backend.enums.ClassMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMemberRoleRequest {

    @NotNull
    ClassMemberRole role;
}
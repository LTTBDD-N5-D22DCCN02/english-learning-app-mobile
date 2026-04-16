package com.estudy.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {

    @Size(max = 100, message = "FULLNAME_INVALID")
    String fullName;

    @Email(message = "EMAIL_INVALID")
    String email;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "PHONE_INVALID")
    String phone;

    LocalDate dob;
}

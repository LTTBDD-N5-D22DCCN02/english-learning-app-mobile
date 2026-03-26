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
public class RegisterRequest {

    @NotBlank(message = "USERNAME_REQUIRED")
    @Size(min = 3, max = 50, message = "USERNAME_INVALID")
    String username;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, max = 100, message = "PASSWORD_INVALID")
    String password;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    String email;

    @NotBlank(message = "FULLNAME_REQUIRED")
    @Size(max = 100, message = "FULLNAME_INVALID")
    String fullName;

    @NotBlank(message = "PHONE_REQUIRED")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "PHONE_INVALID")
    String phone;

    @NotNull(message = "DOB_REQUIRED")
    LocalDate dob;
}
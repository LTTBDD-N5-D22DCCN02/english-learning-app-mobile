package com.estudy.backend.controller;

import com.estudy.backend.dto.request.UpdateProfileRequest;
import com.estudy.backend.dto.response.ApiResponse;
import com.estudy.backend.dto.response.UserResponse;
import com.estudy.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    // UC-01: GET /users/me
    @GetMapping("/me")
    ApiResponse<UserResponse> getMyProfile() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyProfile())
                .build();
    }

    // UC-02: PUT /users/me
    @PutMapping("/me")
    ApiResponse<UserResponse> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateProfile(request))
                .build();
    }
}

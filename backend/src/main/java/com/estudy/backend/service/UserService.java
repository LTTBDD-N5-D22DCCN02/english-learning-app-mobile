package com.estudy.backend.service;

import com.estudy.backend.dto.request.UpdateProfileRequest;
import com.estudy.backend.dto.response.UserResponse;
import com.estudy.backend.entity.User;
import com.estudy.backend.exception.AppException;
import com.estudy.backend.exception.ErrorCode;
import com.estudy.backend.mapper.UserMapper;
import com.estudy.backend.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // UC-01: Xem thông tin cá nhân
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return userMapper.toUserResponse(getCurrentUser());
    }

    // UC-02: Sửa thông tin cá nhân
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                throw new AppException(ErrorCode.USER_EXISTED);
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhoneAndDeletedFalse(request.getPhone()))
                throw new AppException(ErrorCode.USER_EXISTED);
            user.setPhone(request.getPhone());
        }

        if (request.getFullName() != null && !request.getFullName().isBlank())
            user.setFullName(request.getFullName());

        if (request.getDob() != null)
            user.setDob(request.getDob());

        return userMapper.toUserResponse(userRepository.save(user));
    }
}

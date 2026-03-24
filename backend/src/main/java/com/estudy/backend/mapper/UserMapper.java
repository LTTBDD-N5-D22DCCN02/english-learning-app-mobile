package com.estudy.backend.mapper;

import com.estudy.backend.dto.request.RegisterRequest;
import com.estudy.backend.dto.response.UserResponse;
import com.estudy.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "currentStreak", ignore = true)
    @Mapping(target = "longestStreak", ignore = true)
    @Mapping(target = "lastStudyDate", ignore = true)
    @Mapping(target = "flashCardSets", ignore = true)
    User toUser(RegisterRequest request);

    UserResponse toUserResponse(User user);
}
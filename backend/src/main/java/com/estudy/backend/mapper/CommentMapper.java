package com.estudy.backend.mapper;

import com.estudy.backend.dto.request.CommentRequest;
import com.estudy.backend.dto.response.CommentResponse;
import com.estudy.backend.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "flashCardSet", ignore = true)
    @Mapping(target = "user", ignore = true)
    Comment toComment(CommentRequest request);

    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName")
    CommentResponse toCommentResponse(Comment comment);
}
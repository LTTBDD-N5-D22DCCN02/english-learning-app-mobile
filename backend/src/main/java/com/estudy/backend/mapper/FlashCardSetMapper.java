package com.estudy.backend.mapper;

import com.estudy.backend.dto.request.FlashCardSetRequest;
import com.estudy.backend.dto.response.CommentResponse;
import com.estudy.backend.dto.response.FlashCardResponse;
import com.estudy.backend.dto.response.FlashCardSetDetailResponse;
import com.estudy.backend.dto.response.FlashCardSetResponse;
import com.estudy.backend.entity.Comment;
import com.estudy.backend.entity.FlashCard;
import com.estudy.backend.entity.FlashCardSet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FlashCardSetMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "flashCards", ignore = true)
    @Mapping(target = "comments", ignore = true)
    FlashCardSet toFlashCardSet(FlashCardSetRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "flashCards", ignore = true)
    @Mapping(target = "comments", ignore = true)
    void updateFlashCardSet(FlashCardSetRequest request, @MappingTarget FlashCardSet flashCardSet);

    FlashCardSetResponse toFlashCardSetResponse(FlashCardSet flashCardSet);

    FlashCardSetDetailResponse toFlashCardSetDetailResponse(FlashCardSet flashCardSet);

    FlashCardResponse toFlashCardResponse(FlashCard flashCard);

    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName")
    CommentResponse toCommentResponse(Comment comment);
}
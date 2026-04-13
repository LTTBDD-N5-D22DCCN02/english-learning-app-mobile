package com.estudy.backend.mapper;

import com.estudy.backend.dto.request.FlashCardRequest;
import com.estudy.backend.dto.response.FlashCardResponse;
import com.estudy.backend.entity.FlashCard;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface FlashCardMapper {


    FlashCardResponse toResponse(FlashCard flashCard);

    @Mapping(target = "id", ignore = true)
    FlashCard toFlashCard(FlashCardRequest request);
}
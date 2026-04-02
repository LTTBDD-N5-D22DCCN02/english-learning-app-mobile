package com.estudy.backend.mapper;

import com.estudy.backend.dto.response.ClassMemberResponse;
import com.estudy.backend.dto.response.ClassResponse;
import com.estudy.backend.entity.Class;
import com.estudy.backend.entity.ClassMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClassMapper {

    // memberCount và myRole được set thủ công trong service
    @Mapping(target = "memberCount", ignore = true)
    @Mapping(target = "myRole", ignore = true)
    ClassResponse toClassResponse(Class aClass);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName")
    ClassMemberResponse toClassMemberResponse(ClassMember member);
}
package com.estudy.backend.repository;

import com.estudy.backend.entity.Class;
import com.estudy.backend.enums.Privacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassRepository extends JpaRepository<Class, UUID> {
    Optional<Class> findByCodeAndDeletedFalse(String code);
    Optional<Class> findByIdAndDeletedFalse(UUID id);
    // UC-12: Lấy danh sách lớp học công khai
    List<Class> findByPrivacyAndDeletedFalse(Privacy privacy);

    // Tìm kiếm lớp công khai theo từ khóa (tên lớp)
    @Query("SELECT c FROM Class c WHERE c.privacy = :privacy AND c.deleted = false " +
            "AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Class> searchPublicClasses(@Param("privacy") Privacy privacy, @Param("keyword") String keyword);
}
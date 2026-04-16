package com.estudy.backend.repository;

import com.estudy.backend.entity.FlashCardSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.estudy.backend.entity.Class;

@Repository
public interface FlashCardSetRepository extends JpaRepository<FlashCardSet, UUID> {

    List<FlashCardSet> findAllByUserIdAndDeletedFalse(UUID userId);

    Optional<FlashCardSet> findByIdAndDeletedFalse(UUID id);
    // Lấy tất cả flashcard sets của một lớp (chưa bị xóa)
    List<FlashCardSet> findByClazzAndDeletedFalse(Class clazz);

    // Tìm kiếm theo tên trong lớp
    @Query("SELECT f FROM FlashCardSet f WHERE f.clazz = :clazz AND f.deleted = false " +
            "AND LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<FlashCardSet> searchByClassAndName(@Param("clazz") Class clazz, @Param("keyword") String keyword);

    // Tìm flashcard set theo id và class (để kiểm tra quyền)
    Optional<FlashCardSet> findByIdAndClazzAndDeletedFalse(UUID id, Class clazz);
}
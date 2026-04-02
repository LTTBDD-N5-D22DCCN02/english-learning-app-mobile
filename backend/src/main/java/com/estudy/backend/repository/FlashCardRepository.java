package com.estudy.backend.repository;

import com.estudy.backend.entity.FlashCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FlashCardRepository extends JpaRepository<FlashCard, UUID> {

    // Lấy tất cả thẻ trong 1 bộ (chưa bị xóa)
    List<FlashCard> findByFlashCardSetIdAndDeletedFalse(UUID setId);

    // Từ mới chưa học (chưa có StudyRecord của user này)
    @Query("""
        SELECT f FROM FlashCard f
        JOIN f.flashCardSet fs
        WHERE fs.user.id = :userId
          AND f.deleted = false
          AND fs.deleted = false
          AND NOT EXISTS (
              SELECT sr FROM StudyRecord sr
              WHERE sr.flashcard = f AND sr.user.id = :userId
          )
    """)
    List<FlashCard> findNewCardsByUserId(@Param("userId") UUID userId);

    // Từ mới chưa học, lọc theo danh sách setId cụ thể
    @Query("""
        SELECT f FROM FlashCard f
        JOIN f.flashCardSet fs
        WHERE fs.id IN :setIds
          AND f.deleted = false
          AND NOT EXISTS (
              SELECT sr FROM StudyRecord sr
              WHERE sr.flashcard = f AND sr.user.id = :userId
          )
    """)
    List<FlashCard> findNewCardsBySetIds(@Param("userId") UUID userId,
                                         @Param("setIds") List<UUID> setIds);
}
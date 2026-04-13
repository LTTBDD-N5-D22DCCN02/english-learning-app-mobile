package com.estudy.backend.repository;

import com.estudy.backend.entity.FlashCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
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

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FlashCard f " +
            "WHERE f.flashCardSet.id = :setId AND LOWER(f.term) = LOWER(:term) " +
            "AND f.deleted = false AND f.id != :excludeId")
    boolean existsByFlashCardSetIdAndTermIgnoreCaseAndDeletedFalseAndIdNot(
            @Param("setId") UUID setId, @Param("term") String term, @Param("excludeId") UUID excludeId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FlashCard f " +
            "WHERE f.flashCardSet.id = :setId AND LOWER(f.term) = LOWER(:term) AND f.deleted = false")
    boolean existsByFlashCardSetIdAndTermIgnoreCaseAndDeletedFalse(
            @Param("setId") UUID setId, @Param("term") String term);

    @Query("SELECT COUNT(f) FROM FlashCard f WHERE f.flashCardSet.id = :setId AND f.deleted = false")
    long countByFlashCardSetIdAndDeletedFalse(@Param("setId") UUID setId);

    @Query("SELECT f FROM FlashCard f WHERE f.id = :id AND f.deleted = false")
    Optional<FlashCard> findByIdAndDeletedFalse(@Param("id") UUID id);
}
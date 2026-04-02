package com.estudy.backend.repository;

import com.estudy.backend.entity.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, UUID> {

    @Query("""
        SELECT sr FROM StudyRecord sr
        JOIN FETCH sr.flashcard f
        JOIN FETCH f.flashCardSet fs
        WHERE sr.user.id = :userId
          AND sr.nextReviewAt <= :today
          AND f.deleted = false
          AND fs.deleted = false
        ORDER BY sr.nextReviewAt ASC
    """)
    List<StudyRecord> findDueToday(@Param("userId") UUID userId,
                                   @Param("today") LocalDate today);

    @Query("""
        SELECT sr FROM StudyRecord sr
        JOIN FETCH sr.flashcard f
        JOIN FETCH f.flashCardSet fs
        WHERE sr.user.id = :userId
          AND fs.id IN :setIds
          AND sr.nextReviewAt <= :today
          AND f.deleted = false
          AND fs.deleted = false
        ORDER BY sr.nextReviewAt ASC
    """)
    List<StudyRecord> findDueTodayBySetIds(@Param("userId") UUID userId,
                                           @Param("today") LocalDate today,
                                           @Param("setIds") List<UUID> setIds);

    Optional<StudyRecord> findByUserIdAndFlashcardId(UUID userId, UUID flashcardId);

    long countByUserId(UUID userId);

    long countByUserIdAndRememberedTrue(UUID userId);

    @Query("SELECT SUM(sr.correctCount), SUM(sr.wrongCount) FROM StudyRecord sr WHERE sr.user.id = :uid")
    Object[] sumAccuracy(@Param("uid") UUID uid);
}
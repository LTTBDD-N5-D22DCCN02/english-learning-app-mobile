package com.estudy.backend.repository;

import com.estudy.backend.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface QuizSessionRepository extends JpaRepository<QuizSession, UUID> {

    @Query("""
        SELECT qs FROM QuizSession qs
        WHERE qs.user.id = :uid
          AND qs.startedAt >= :from
          AND qs.endedAt IS NOT NULL
        ORDER BY qs.startedAt ASC
    """)
    List<QuizSession> findCompletedSessions(
            @Param("uid") UUID uid,
            @Param("from") LocalDateTime from);
    
    @Query(value = """
    SELECT COUNT(DISTINCT DATE(qs.started_at))
    FROM quiz_session qs
    WHERE qs.user_id = :uid
      AND qs.ended_at IS NOT NULL
    """, nativeQuery = true)
    long countStudyDays(@Param("uid") UUID uid);
}
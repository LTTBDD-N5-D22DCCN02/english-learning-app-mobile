package com.estudy.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "study_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "flashcard_id"}),
        indexes = {
                @Index(name = "idx_study_user_review", columnList = "user_id, next_review_at"),
                @Index(name = "idx_study_user", columnList = "user_id")
        }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_id", nullable = false)
    FlashCard flashcard;

    // Trạng thái học
    @Column(nullable = false)
    @Builder.Default
    Boolean remembered = false;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    Integer correctCount = 0;

    @Column(name = "wrong_count", nullable = false)
    @Builder.Default
    Integer wrongCount = 0;

    @Column(name = "last_studied_at")
    LocalDateTime lastStudiedAt;

    // Thông số SM-2
    @Column(name = "ease_factor", nullable = false)
    @Builder.Default
    Float easeFactor = 2.5f;   // mặc định SM-2

    @Column(name = "interval_days", nullable = false)
    @Builder.Default
    Integer intervalDays = 1;

    @Column(name = "repetition_count", nullable = false)
    @Builder.Default
    Integer repetitionCount = 0;

    @Column(name = "next_review_at")
    LocalDate nextReviewAt;    // null = chưa học lần nào
}
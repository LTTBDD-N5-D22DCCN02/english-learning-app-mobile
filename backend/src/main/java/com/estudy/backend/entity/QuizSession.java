package com.estudy.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quiz_sessions",
        indexes = {
                @Index(name = "idx_quiz_user_started", columnList = "user_id, started_at"),
                @Index(name = "idx_quiz_user", columnList = "user_id")
        }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id", nullable = false)
    FlashCardSet flashcardSet;

    // mode: 'flashcard' | 'word_quiz' | 'match' | 'spelling'
    @Column(nullable = false, length = 20)
    String mode;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    Integer correctCount = 0;

    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    Integer totalQuestions = 0;

    @Column(name = "started_at", nullable = false)
    LocalDateTime startedAt;

    @Column(name = "ended_at")
    LocalDateTime endedAt;     // null = đang học dở
}
package com.estudy.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(nullable = false, length = 50)
    String type;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "is_read")
    @Builder.Default
    boolean isRead = false;

    // reference id (classId, flashcardSetId, etc.)
    @Column(name = "id_ref")
    UUID idRef;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
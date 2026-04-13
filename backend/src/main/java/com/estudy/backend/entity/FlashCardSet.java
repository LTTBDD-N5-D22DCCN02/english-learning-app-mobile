package com.estudy.backend.entity;

import com.estudy.backend.enums.Privacy;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import com.estudy.backend.entity.Class;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "flashcard_sets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlashCardSet {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    UUID id;

    @Column(nullable = false)
    String name;

    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Privacy privacy;

    @Builder.Default
    @Column(nullable = false)
    Boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @OneToMany(mappedBy = "flashCardSet", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Comment> comments;

    @OneToMany(mappedBy = "flashCardSet", cascade = CascadeType.ALL, orphanRemoval = true)
    List<FlashCard> flashCards;

//    @OneToMany(mappedBy = "user")
//    List<QuizSession> quizSessions;
//
//    @OneToMany(mappedBy = "user")
//    List<FlashCard> flashCards;
//

    // THÊM field vào class body (sau field `user`)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    Class clazz;  // null = flashcard set cá nhân, not-null = thuộc lớp học
}

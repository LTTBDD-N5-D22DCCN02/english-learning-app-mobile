package com.estudy.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    UUID id;

    @Column(name = "full_name")
    String fullName;

    @Column(nullable = false, unique = true)
    String username;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    String password;

    @Column(unique = true)
    String phone;

    LocalDate dob;

    @Column(name = "current_streak")
    Integer currentStreak;

    @Column(name = "longest_streak")
    Integer longestStreak;

    @Column(name = "last_study_date")
    LocalDate lastStudyDate;

    @Builder.Default
    @Column(nullable = false)
    Boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @OneToMany(mappedBy = "user")
    List<FlashCardSet> flashCardSets;

    @OneToMany(mappedBy = "user")
    List<Comment> comments;

//    @OneToMany(mappedBy = "user")
//    List<QuizSession> quizSessions;
//
//    @OneToMany(mappedBy = "user")
//    List<ClassMember> classMembers;
//
//    @OneToMany(mappedBy = "user")
//    List<Notification> notifications;
}

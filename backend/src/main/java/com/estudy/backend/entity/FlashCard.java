package com.estudy.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flashcards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlashCard {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    UUID id;

    @Column(nullable = false)
    String term;

    String definition;

    String image;

    String ipa;

    String example;

    @Builder.Default
    @Column(nullable = false)
    Boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "flashcardset_id", nullable = false)
    FlashCardSet flashCardSet;

//    @OneToMany(mappedBy = "studyRecord")
//    List<StudyRecord> studyRecords;

}
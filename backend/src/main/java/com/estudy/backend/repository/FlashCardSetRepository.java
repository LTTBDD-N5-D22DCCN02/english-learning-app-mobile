package com.estudy.backend.repository;

import com.estudy.backend.entity.FlashCardSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashCardSetRepository extends JpaRepository<FlashCardSet, UUID> {

    List<FlashCardSet> findAllByUserIdAndDeletedFalse(UUID userId);

    Optional<FlashCardSet> findByIdAndDeletedFalse(UUID id);
}
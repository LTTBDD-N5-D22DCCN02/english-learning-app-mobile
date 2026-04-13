package com.estudy.backend.service;

import com.estudy.backend.dto.request.FlashCardImportRequest;
import com.estudy.backend.dto.request.FlashCardRequest;
import com.estudy.backend.dto.response.FlashCardResponse;
import com.estudy.backend.dto.response.ImportResultResponse;
import com.estudy.backend.dto.response.SuggestResponse;
import com.estudy.backend.entity.FlashCard;
import com.estudy.backend.entity.FlashCardSet;
import com.estudy.backend.entity.User;
import com.estudy.backend.exception.AppException;
import com.estudy.backend.exception.ErrorCode;
import com.estudy.backend.mapper.FlashCardMapper;
import com.estudy.backend.repository.FlashCardRepository;
import com.estudy.backend.repository.FlashCardSetRepository;
import com.estudy.backend.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlashCardService {

    FlashCardRepository flashCardRepository;
    FlashCardSetRepository flashCardSetRepository;
    FlashCardMapper flashCardMapper;
    DictionaryService dictionaryService;
    UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // ─── UC-03: Get all flashcards in a set ───────────────────────────────────

    public List<FlashCardResponse> getFlashCardsBySet(UUID setId) {
        User currentUser = getCurrentUser();
        FlashCardSet set = getSetOrThrow(setId);

        if(!set.getUser().getId().equals(currentUser.getId()) && !"PUBLIC".equals(set.getPrivacy())) {
            throw new AppException(ErrorCode.FLASHCARD_SET_NOT_OWNED);
        }
        return flashCardRepository.findByFlashCardSetIdAndDeletedFalse(setId)
                .stream()
                .map(flashCardMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ─── UC-01: Add single flashcard ─────────────────────────────────────────

    @Transactional
    public FlashCardResponse createFlashCard(UUID setId, FlashCardRequest request) {
        FlashCardSet set = getSetOrThrow(setId);
        User currentUser = getCurrentUser();

        if(!set.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FLASHCARD_SET_NOT_OWNED);
        }

        // Validate unique term within set
        if (flashCardRepository.existsByFlashCardSetIdAndTermIgnoreCaseAndDeletedFalse(setId, request.getTerm())) {
            throw new IllegalArgumentException("A flashcard with this term already exists in the set");
        }

        FlashCard flashcard = FlashCard.builder()
                .term(request.getTerm().trim())
                .definition(request.getDefinition())
                .ipa(request.getIpa())
                .example(request.getExample())
                .image(request.getImage())
                .flashCardSet(set)
                .build();

        return flashCardMapper.toResponse(flashCardRepository.save(flashcard));
    }

    // ─── UC-02: Import batch flashcards ──────────────────────────────────────

    @Transactional
    public ImportResultResponse importFlashcards(UUID setId, FlashCardImportRequest request) {
        FlashCardSet set = getSetOrThrow(setId);
        User currentUser = getCurrentUser();

        if(!set.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FLASHCARD_SET_NOT_OWNED);
        }

        String content = request.getContent().trim();
        String[] lines = content.split("\\r?\\n");

        List<FlashCard> toSave = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int failedCount = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            int commaIdx = line.indexOf(',');
            if (commaIdx < 0) {
                errors.add("Line " + (i + 1) + ": missing comma separator — skipped");
                failedCount++;
                continue;
            }

            String term = line.substring(0, commaIdx).trim();
            String definition = line.substring(commaIdx + 1).trim();

            if (term.isEmpty()) {
                errors.add("Line " + (i + 1) + ": empty term — skipped");
                failedCount++;
                continue;
            }

            // Check duplicate within this batch
            boolean dupInBatch = toSave.stream()
                    .anyMatch(f -> f.getTerm().equalsIgnoreCase(term));
            if (dupInBatch) {
                errors.add("Line " + (i + 1) + ": duplicate term '" + term + "' in batch — skipped");
                failedCount++;
                continue;
            }

            // Check duplicate in existing DB
            if (flashCardRepository.existsByFlashCardSetIdAndTermIgnoreCaseAndDeletedFalse(setId, term)) {
                errors.add("Line " + (i + 1) + ": term '" + term + "' already exists — skipped");
                failedCount++;
                continue;
            }

            toSave.add(FlashCard.builder()
                    .term(term)
                    .definition(definition)
                    .flashCardSet(set)
                    .build());
        }

        List<FlashCard> saved = flashCardRepository.saveAll(toSave);

        return ImportResultResponse.builder()
                .successCount(saved.size())
                .failedCount(failedCount)
                .errors(errors)
                .importedCards(saved.stream().map(flashCardMapper::toResponse).collect(Collectors.toList()))
                .build();
    }

    // ─── UC-04: Update flashcard ──────────────────────────────────────────────

    @Transactional
    public FlashCardResponse updateFlashCard(UUID flashCardId, FlashCardRequest request) {
        FlashCard flashcard = getFlashCardOrThrow(flashCardId);
        User currentUser = getCurrentUser();
        if(!flashcard.getFlashCardSet().getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FLASHCARD_SET_NOT_OWNED);
        }

        // Check term uniqueness (exclude current card)
        if (flashCardRepository.existsByFlashCardSetIdAndTermIgnoreCaseAndDeletedFalseAndIdNot(
                flashcard.getFlashCardSet().getId(), request.getTerm(), flashCardId)) {
            throw new IllegalArgumentException("A flashcard with this term already exists in the set");
        }

        flashcard.setTerm(request.getTerm().trim());
        flashcard.setDefinition(request.getDefinition());
        flashcard.setIpa(request.getIpa());
        flashcard.setExample(request.getExample());
        if (request.getImage() != null) {
            flashcard.setImage(request.getImage());
        }

        return flashCardMapper.toResponse(flashCardRepository.save(flashcard));
    }

    // ─── UC-05: Delete FlashCard (soft delete) ────────────────────────────────

    @Transactional
    public void deleteFlashCard(UUID flashcardId) {
        FlashCard flashcard = getFlashCardOrThrow(flashcardId);
        User currentUser = getCurrentUser();
        if(!flashcard.getFlashCardSet().getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FLASHCARD_SET_NOT_OWNED);
        }
        flashcard.setDeleted(true);
        flashCardRepository.save(flashcard);
    }

    // ─── UC-06: Suggest definition / example ─────────────────────────────────

    public SuggestResponse suggestForTerm(String term) {
        return dictionaryService.lookup(term);
    }

    // ─── Helper methods ───────────────────────────────────────────────────────

    private FlashCardSet getSetOrThrow(UUID setId) {
        return flashCardSetRepository.findByIdAndDeletedFalse(setId)
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_SET_NOT_FOUND));
    }

    private FlashCard getFlashCardOrThrow(UUID id) {
        return flashCardRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_NOT_EXISTED));
    }

}
package com.estudy.backend.service;

import com.estudy.backend.dto.response.StudySetItemResponse;
import com.estudy.backend.dto.response.StudyTodayResponse;
import com.estudy.backend.entity.FlashCard;
import com.estudy.backend.entity.FlashCardSet;
import com.estudy.backend.entity.StudyRecord;
import com.estudy.backend.exception.AppException;
import com.estudy.backend.exception.ErrorCode;
import com.estudy.backend.repository.FlashCardRepository;
import com.estudy.backend.repository.StudyRecordRepository;
import com.estudy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final StudyRecordRepository studyRecordRepository;
    private final FlashCardRepository flashCardRepository;
    private final UserRepository userRepository;

    // ── UC-STUDY-01: Lấy danh sách từ cần ôn hôm nay ─────────────
    public StudyTodayResponse getStudyToday() {
        UUID userId = getCurrentUserId();
        LocalDate today = LocalDate.now();

        // 1. Từ cần ôn (có StudyRecord, nextReviewAt <= today)
        List<StudyRecord> dueRecords =
                studyRecordRepository.findDueToday(userId, today);

        // 2. Từ mới chưa học — dùng query trong FlashCardRepository
        List<FlashCard> newCards =
                flashCardRepository.findNewCardsByUserId(userId);

        // 3. Nhóm theo setId
        Map<UUID, List<StudyRecord>> dueBySet = dueRecords.stream()
                .collect(Collectors.groupingBy(
                        sr -> sr.getFlashcard().getFlashCardSet().getId()));

        Map<UUID, List<FlashCard>> newBySet = newCards.stream()
                .collect(Collectors.groupingBy(
                        fc -> fc.getFlashCardSet().getId()));

        // 4. Build response
        List<StudySetItemResponse> dueSets = buildDueItems(dueBySet);
        List<StudySetItemResponse> newSets  = buildNewItems(newBySet);

        return new StudyTodayResponse(dueRecords.size(), newCards.size(), dueSets, newSets);
    }

    private List<StudySetItemResponse> buildDueItems(Map<UUID, List<StudyRecord>> map) {
        return map.entrySet().stream()
                .map(e -> {
                    List<StudyRecord> records = e.getValue();
                    FlashCardSet set = records.get(0).getFlashcard().getFlashCardSet();

                    List<String> preview = records.stream()
                            .limit(3)
                            .map(sr -> sr.getFlashcard().getTerm())
                            .collect(Collectors.toList());

                    long remembered = records.stream()
                            .filter(sr -> Boolean.TRUE.equals(sr.getRemembered()))
                            .count();

                    String lastReviewed = records.stream()
                            .map(StudyRecord::getLastStudiedAt)
                            .filter(Objects::nonNull)
                            .max(Comparator.naturalOrder())
                            .map(dt -> formatDate(dt.toLocalDate()))
                            .orElse("Never studied");

                    int totalWords = (int) set.getFlashCards().stream()
                            .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                            .count();

                    return new StudySetItemResponse(
                            set.getId().toString(), set.getName(),
                            records.size(), totalWords,
                            (int) remembered, lastReviewed, preview);
                })
                .collect(Collectors.toList());
    }

    private List<StudySetItemResponse> buildNewItems(Map<UUID, List<FlashCard>> map) {
        return map.entrySet().stream()
                .map(e -> {
                    List<FlashCard> cards = e.getValue();
                    FlashCardSet set = cards.get(0).getFlashCardSet();

                    List<String> preview = cards.stream()
                            .limit(3)
                            .map(FlashCard::getTerm)
                            .collect(Collectors.toList());

                    int totalWords = (int) set.getFlashCards().stream()
                            .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                            .count();

                    return new StudySetItemResponse(
                            set.getId().toString(), set.getName(),
                            cards.size(), totalWords, 0,
                            "Never studied", preview);
                })
                .collect(Collectors.toList());
    }

    private String formatDate(LocalDate date) {
        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days == 0) return "Today";
        if (days == 1) return "Yesterday";
        return days + " days ago";
    }

    private UUID getCurrentUserId() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED))
                .getId();
    }
}
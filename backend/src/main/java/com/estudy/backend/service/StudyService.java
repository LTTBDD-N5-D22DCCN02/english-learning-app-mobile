package com.estudy.backend.service;

import com.estudy.backend.dto.request.AnswerRequest;
import com.estudy.backend.dto.response.StudySetItemResponse;
import com.estudy.backend.dto.response.StudyTodayResponse;
import com.estudy.backend.entity.FlashCard;
import com.estudy.backend.entity.FlashCardSet;
import com.estudy.backend.entity.StudyRecord;
import com.estudy.backend.entity.User;
import com.estudy.backend.exception.AppException;
import com.estudy.backend.exception.ErrorCode;
import com.estudy.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final StudyRecordRepository  studyRecordRepository;
    private final QuizSessionRepository  quizSessionRepository;
    private final FlashCardRepository    flashCardRepository;
    private final FlashCardSetRepository flashCardSetRepository;
    private final UserRepository         userRepository;

    // ── UC-STUDY-01 ──
    public StudyTodayResponse getStudyToday() {
        UUID userId = getCurrentUserId();
        LocalDate today = LocalDate.now();

        List<StudyRecord> dueRecords =
                studyRecordRepository.findDueToday(userId, today);

        List<FlashCard> newCards =
                flashCardRepository.findNewCardsByUserId(userId);

        Map<UUID, List<StudyRecord>> dueBySet = dueRecords.stream()
                .collect(Collectors.groupingBy(sr -> sr.getFlashcard().getFlashCardSet().getId()));
        Map<UUID, List<FlashCard>>   newBySet = newCards.stream()
                .collect(Collectors.groupingBy(fc -> fc.getFlashCardSet().getId()));

        return new StudyTodayResponse(dueRecords.size(), newCards.size(),
                buildDueItems(dueBySet), buildNewItems(newBySet));
    }

    // ── UC-STUDY-02~05: Start session ──
    @Transactional
    public StartSessionResponse startSession(StartSessionRequest req) {
        UUID userId = getCurrentUserId();
        User user   = getUser(userId);

        FlashCardSet set = flashCardSetRepository.findByIdAndDeletedFalse(req.getSetId())
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_SET_NOT_FOUND));

        LocalDate today = LocalDate.now();
        List<FlashCard> dueCards = studyRecordRepository
                .findDueTodayBySetIds(userId, today, List.of(set.getId()))
                .stream().map(StudyRecord::getFlashcard).collect(Collectors.toList());
        List<FlashCard> newCards = flashCardRepository
                .findNewCardsBySetIds(userId, List.of(set.getId()));

        List<FlashCard> cards = new ArrayList<>(dueCards);
        cards.addAll(newCards);
        if (cards.isEmpty())
            cards = flashCardRepository.findByFlashCardSetIdAndDeletedFalse(set.getId());

        List<StudySetItemResponse> dueSets = buildDueItems(dueBySet);
        List<StudySetItemResponse> newSets  = buildNewItems(newBySet);

        return StartSessionResponse.builder()
                .sessionId(session.getId().toString())
                .setId(set.getId().toString())
                .setName(set.getName())
                .mode(req.getMode())
                .cards(buildCardResponses(cards, req.getMode(), set.getId()))
                .build();
    }

    // ── UC-STUDY-02: Ghi nhận kết quả trả lời + cập nhật SM-2 ─────
    @Transactional
    public void submitAnswer(AnswerRequest request) {
        UUID userId = getCurrentUserId();
        User user = userRepository.findByUsernameAndDeletedFalse(
                SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UUID flashcardId = UUID.fromString(request.getFlashcardId());
        FlashCard flashcard = flashCardRepository.findById(flashcardId)
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_NOT_EXISTED));

        boolean correct = Boolean.TRUE.equals(request.getCorrect());

        // Tìm hoặc tạo StudyRecord
        StudyRecord record = studyRecordRepository
                .findByUserIdAndFlashcardId(userId, flashcardId)
                .orElse(StudyRecord.builder()
                        .user(user)
                        .flashcard(flashcard)
                        .build());

        // Áp dụng thuật toán SM-2
        applySM2(record, correct);

        studyRecordRepository.save(record);
    }

    // ── SM-2 Algorithm ──────────────────────────────────────────────
    private void applySM2(StudyRecord record, boolean correct) {
        // Quality: 4 = correct, 0 = wrong (simplified)
        int quality = correct ? 4 : 0;

        float ef = record.getEaseFactor() != null ? record.getEaseFactor() : 2.5f;
        int interval = record.getIntervalDays() != null ? record.getIntervalDays() : 1;
        int rep = record.getRepetitionCount() != null ? record.getRepetitionCount() : 0;

        if (quality < 3) {
            // Wrong: reset repetition
            rep = 0;
            interval = 1;
        } else {
            // Correct: update EF and interval
            ef += 0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f);
            ef = Math.max(1.3f, ef);

            if (rep == 0) {
                interval = 1;
            } else if (rep == 1) {
                interval = 6;
            } else {
                interval = Math.round(interval * ef);
            }
            rep += 1;
        }

        record.setEaseFactor(ef);
        record.setIntervalDays(interval);
        record.setRepetitionCount(rep);
        record.setNextReviewAt(LocalDate.now().plusDays(interval));
        record.setLastStudiedAt(LocalDateTime.now());
        record.setRemembered(correct);

        if (correct) {
            record.setCorrectCount((record.getCorrectCount() != null ? record.getCorrectCount() : 0) + 1);
        } else {
            record.setWrongCount((record.getWrongCount() != null ? record.getWrongCount() : 0) + 1);
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────
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

    // ── Helpers ──
    private int[] updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate last  = user.getLastStudyDate();
        if (last == null)                        user.setCurrentStreak(1);
        else if (last.equals(today))             { /* no-op */ }
        else if (last.equals(today.minusDays(1))) user.setCurrentStreak(user.getCurrentStreak() + 1);
        else                                     user.setCurrentStreak(1);
        user.setLastStudyDate(today);
        int isNew = 0;
        if (user.getCurrentStreak() > user.getLongestStreak()) {
            user.setLongestStreak(user.getCurrentStreak()); isNew = 1;
        }
        return new int[]{user.getCurrentStreak(), isNew};
    }

    private void applySM2(StudyRecord r, boolean correct) {
        if (correct) {
            int n = r.getRepetitionCount();
            int interval = n == 0 ? 1 : n == 1 ? 6 : Math.round(r.getIntervalDays() * r.getEaseFactor());
            r.setEaseFactor(Math.max(1.3f, r.getEaseFactor() + 0.1f));
            r.setIntervalDays(interval);
            r.setRepetitionCount(n + 1);
        } else {
            r.setEaseFactor(Math.max(1.3f, r.getEaseFactor() - 0.2f));
            r.setIntervalDays(1);
            r.setRepetitionCount(0);
        }
        r.setNextReviewAt(LocalDate.now().plusDays(r.getIntervalDays()));
    }

    private boolean determineCorrect(String mode, AnswerRequest req) {
        return "flashcard".equals(mode)
                ? Boolean.TRUE.equals(req.getRemembered())
                : Boolean.TRUE.equals(req.getCorrect());
    }

    private List<StartSessionResponse.SessionCardResponse> buildCardResponses(
            List<FlashCard> cards, String mode, UUID setId) {
        List<String> allDefs = "word_quiz".equals(mode)
                ? flashCardRepository.findByFlashCardSetIdAndDeletedFalse(setId)
                  .stream().map(FlashCard::getDefinition).filter(Objects::nonNull)
                  .collect(Collectors.toList())
                : List.of();

        return cards.stream().map(fc -> {
            List<String> distractors = List.of();
            if ("word_quiz".equals(mode)) {
                List<String> pool = allDefs.stream()
                        .filter(d -> !d.equals(fc.getDefinition()))
                        .collect(Collectors.toList());
                Collections.shuffle(pool);
                distractors = pool.stream().limit(3).collect(Collectors.toList());
            }
            return StartSessionResponse.SessionCardResponse.builder()
                    .flashcardId(fc.getId().toString()).term(fc.getTerm())
                    .definition(fc.getDefinition()).ipa(fc.getIpa())
                    .audioUrl(null) // ⚠ Cần API TTS — xem ghi chú bên dưới
                    .image(fc.getImage()).example(fc.getExample())
                    .distractors(distractors).build();
        }).collect(Collectors.toList());
    }

    private List<StudySetItemResponse> buildDueItems(Map<UUID, List<StudyRecord>> map) {
        return map.entrySet().stream().map(e -> {
            List<StudyRecord> records = e.getValue();
            FlashCardSet set = records.get(0).getFlashcard().getFlashCardSet();
            List<String> preview = records.stream().limit(3)
                    .map(sr -> sr.getFlashcard().getTerm()).collect(Collectors.toList());
            long rem = records.stream().filter(sr -> Boolean.TRUE.equals(sr.getRemembered())).count();
            String last = records.stream().map(StudyRecord::getLastStudiedAt)
                    .filter(Objects::nonNull).max(Comparator.naturalOrder())
                    .map(dt -> formatDate(dt.toLocalDate())).orElse("Never studied");
            int total = (int) set.getFlashCards().stream()
                    .filter(f -> !Boolean.TRUE.equals(f.getDeleted())).count();
            return new StudySetItemResponse(set.getId().toString(), set.getName(),
                    records.size(), total, (int) rem, last, preview);
        }).collect(Collectors.toList());
    }

    private List<StudySetItemResponse> buildNewItems(Map<UUID, List<FlashCard>> map) {
        return map.entrySet().stream().map(e -> {
            List<FlashCard> cards = e.getValue();
            FlashCardSet set = cards.get(0).getFlashCardSet();
            List<String> preview = cards.stream().limit(3)
                    .map(FlashCard::getTerm).collect(Collectors.toList());
            int total = (int) set.getFlashCards().stream()
                    .filter(f -> !Boolean.TRUE.equals(f.getDeleted())).count();
            return new StudySetItemResponse(set.getId().toString(), set.getName(),
                    cards.size(), total, 0, "Never studied", preview);
        }).collect(Collectors.toList());
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

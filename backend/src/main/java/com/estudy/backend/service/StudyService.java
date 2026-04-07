package com.estudy.backend.service;

import com.estudy.backend.dto.request.AnswerRequest;
import com.estudy.backend.dto.request.StartSessionRequest;
import com.estudy.backend.dto.response.*;
import com.estudy.backend.entity.*;
import com.estudy.backend.exception.AppException;
import com.estudy.backend.exception.ErrorCode;
import com.estudy.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        List<StudyRecord> dueRecords = studyRecordRepository.findDueToday(userId, today);
        List<FlashCard>   newCards   = flashCardRepository.findNewCardsByUserId(userId);

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

        QuizSession session = quizSessionRepository.save(QuizSession.builder()
                .user(user)
                .flashcardSet(set)
                .mode(req.getMode())
                .totalQuestions(cards.size())
                .startedAt(LocalDateTime.now())
                .build());

        return StartSessionResponse.builder()
                .sessionId(session.getId().toString())
                .setId(set.getId().toString())
                .setName(set.getName())
                .mode(req.getMode())
                .cards(buildCardResponses(cards, req.getMode(), set.getId()))
                .build();
    }

    // ── UC-STUDY-02~05: Submit answer ──
    @Transactional
    public void submitAnswer(AnswerRequest req) {
        UUID userId = getCurrentUserId();
        User user   = getUser(userId);

        QuizSession session = quizSessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
        FlashCard card = flashCardRepository.findById(req.getFlashcardId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        StudyRecord record = studyRecordRepository
                .findByUserIdAndFlashcardId(userId, card.getId())
                .orElse(StudyRecord.builder().user(user).flashcard(card).build());

        boolean correct = determineCorrect(session.getMode(), req);

        if (correct) {
            record.setCorrectCount(record.getCorrectCount() + 1);
            if ("flashcard".equals(session.getMode()))
                record.setRemembered(Boolean.TRUE.equals(req.getRemembered()));
        } else {
            record.setWrongCount(record.getWrongCount() + 1);
            if ("flashcard".equals(session.getMode()))
                record.setRemembered(false);
        }

        applySM2(record, correct);
        record.setLastStudiedAt(LocalDateTime.now());
        studyRecordRepository.save(record);
    }

    // ── UC-STUDY-02~05: End session ──
    @Transactional
    public SessionResultResponse endSession(UUID sessionId, List<String> wrongTerms) {
        UUID userId = getCurrentUserId();

        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        LocalDateTime now = LocalDateTime.now();
        session.setEndedAt(now);
        int wrongCount = wrongTerms != null ? wrongTerms.size() : 0;
        int correct    = session.getTotalQuestions() - wrongCount;
        session.setCorrectCount(correct);
        quizSessionRepository.save(session);

        // UC-STUDY-06: streak
        User user = getUser(userId);
        int[] streak = updateStreak(user);
        userRepository.save(user);

        int duration = (int) ChronoUnit.SECONDS.between(session.getStartedAt(), now);

        return SessionResultResponse.builder()
                .sessionId(sessionId.toString())
                .mode(session.getMode())
                .correctCount(correct)
                .totalQuestions(session.getTotalQuestions())
                .durationSeconds(duration)
                .currentStreak(streak[0])
                .isNewRecord(streak[1] == 1)
                .wrongTerms(wrongTerms != null ? wrongTerms : List.of())
                .build();
    }

    // ── UC-STAT-01,02,03: Summary ──
    public StatSummaryResponse getStatSummary() {
        UUID userId = getCurrentUserId();
        User user   = getUser(userId);

        long learned  = studyRecordRepository.countByUserId(userId);
        long mastered = studyRecordRepository.countByUserIdAndRememberedTrue(userId);

        Object[] acc   = studyRecordRepository.sumAccuracy(userId);
        long totalAns  = acc[0] != null ? ((Number) acc[0]).longValue() : 0;
        long wrongAns  = acc[1] != null ? ((Number) acc[1]).longValue() : 0;
        double accuracy = totalAns > 0
                ? Math.round((totalAns - wrongAns) * 1000.0 / totalAns) / 10.0 : 0.0;

        return StatSummaryResponse.builder()
                .wordsLearned((int) learned).wordsMastered((int) mastered)
                .totalAnswers(totalAns).wrongAnswers(wrongAns).accuracyPercent(accuracy)
                .currentStreak(user.getCurrentStreak()).longestStreak(user.getLongestStreak())
                .isNewRecord(user.getCurrentStreak() > 0
                        && user.getCurrentStreak().equals(user.getLongestStreak()))
                .build();
    }

    // ── UC-STAT-04,05: Activity ──
    public List<DayActivityResponse> getStudyActivity(String period) {
        UUID userId = getCurrentUserId();
        int days = "monthly".equals(period) ? 30 : 7;
        LocalDateTime from = LocalDateTime.now().minusDays(days).toLocalDate().atStartOfDay();

        List<QuizSession> sessions = quizSessionRepository.findCompletedSessions(userId, from);
        Map<String, List<QuizSession>> byDate = sessions.stream().collect(
                Collectors.groupingBy(s -> s.getStartedAt().toLocalDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE)));

        List<DayActivityResponse> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String dateStr = LocalDate.now().minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            List<QuizSession> day = byDate.getOrDefault(dateStr, List.of());
            int wc = day.stream().mapToInt(QuizSession::getTotalQuestions).sum();
            int tq = day.stream().mapToInt(QuizSession::getTotalQuestions).sum();
            int cq = day.stream().mapToInt(QuizSession::getCorrectCount).sum();
            double acc = tq > 0 ? Math.round(cq * 1000.0 / tq) / 10.0 : 0.0;
            result.add(DayActivityResponse.builder().date(dateStr).wordCount(wc).accuracyPercent(acc).build());
        }
        return result;
    }

    // ── UC-STAT-06: Set Progress ──
    public List<SetProgressResponse> getSetProgress() {
        UUID userId = getCurrentUserId();
        List<FlashCardSet> sets = flashCardSetRepository.findAllByUserIdAndDeletedFalse(userId);

        return sets.stream().map(set -> {
                    List<FlashCard> cards = set.getFlashCards().stream()
                            .filter(f -> !Boolean.TRUE.equals(f.getDeleted())).collect(Collectors.toList());
                    if (cards.isEmpty()) return null;

                    int remembered = 0, notYet = 0, notStudied = 0;
                    for (FlashCard fc : cards) {
                        Optional<StudyRecord> sr =
                                studyRecordRepository.findByUserIdAndFlashcardId(userId, fc.getId());
                        if (sr.isEmpty()) notStudied++;
                        else if (Boolean.TRUE.equals(sr.get().getRemembered())) remembered++;
                        else notYet++;
                    }
                    double pct = Math.round(remembered * 1000.0 / cards.size()) / 10.0;

                    return SetProgressResponse.builder()
                            .setId(set.getId().toString()).setName(set.getName())
                            .totalWords(cards.size()).rememberedCount(remembered)
                            .notYetCount(notYet).notStudiedCount(notStudied).percentage(pct).build();
                }).filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(SetProgressResponse::getPercentage).reversed())
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

    private UUID getCurrentUserId() { return getUser(
            SecurityContextHolder.getContext().getAuthentication().getName()).getId(); }
    private User getUser(UUID id) { return userRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)); }
    private User getUser(String username) { return userRepository.findByUsernameAndDeletedFalse(username)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)); }
}
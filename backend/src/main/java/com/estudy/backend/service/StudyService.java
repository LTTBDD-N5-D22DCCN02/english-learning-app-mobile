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

    // ── UC-STUDY-01: Study Today screen ──────────────────────────────
    public StudyTodayResponse getStudyToday() {
        UUID userId = getCurrentUserId();
        LocalDate today = LocalDate.now();

        List<StudyRecord> dueRecords =
                studyRecordRepository.findDueToday(userId, today);
        List<FlashCard> newCards =
                flashCardRepository.findNewCardsByUserId(userId);

        Map<UUID, List<StudyRecord>> dueBySet = dueRecords.stream()
                .collect(Collectors.groupingBy(sr -> sr.getFlashcard().getFlashCardSet().getId()));
        Map<UUID, List<FlashCard>> newBySet = newCards.stream()
                .collect(Collectors.groupingBy(fc -> fc.getFlashCardSet().getId()));

        return new StudyTodayResponse(dueRecords.size(), newCards.size(),
                buildDueItems(dueBySet), buildNewItems(newBySet));
    }

    // ── UC-STUDY-02~05: Start session ─────────────────────────────────
    @Transactional
    public StartSessionResponse startSession(StartSessionRequest req) {
        UUID userId = getCurrentUserId();
        User user   = getCurrentUser();

        FlashCardSet set = flashCardSetRepository.findByIdAndDeletedFalse(req.getSetId())
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_SET_NOT_FOUND));

        LocalDate today = LocalDate.now();

        // Lấy thẻ cần ôn tập hôm nay
        List<FlashCard> dueCards = studyRecordRepository
                .findDueTodayBySetIds(userId, today, List.of(set.getId()))
                .stream().map(StudyRecord::getFlashcard).collect(Collectors.toList());

        // Lấy thẻ mới chưa học
        List<FlashCard> newCards = flashCardRepository
                .findNewCardsBySetIds(userId, List.of(set.getId()));

        // Gộp lại
        List<FlashCard> cards = new ArrayList<>(dueCards);
        cards.addAll(newCards);

        // Nếu không có thẻ due/mới, lấy toàn bộ thẻ trong bộ
        if (cards.isEmpty()) {
            cards = flashCardRepository.findByFlashCardSetIdAndDeletedFalse(set.getId());
        }

        // Tạo QuizSession để track phiên học
        QuizSession session = QuizSession.builder()
                .user(user)
                .flashcardSet(set)
                .mode(req.getMode())
                .totalQuestions(cards.size())
                .startedAt(LocalDateTime.now())
                .build();
        quizSessionRepository.save(session);

        return StartSessionResponse.builder()
                .sessionId(session.getId().toString())
                .setId(set.getId().toString())
                .setName(set.getName())
                .mode(req.getMode())
                .cards(buildCardResponses(cards, req.getMode(), set.getId()))
                .build();
    }

    // ── UC-STUDY-02~05: Ghi nhận câu trả lời + cập nhật SM-2 ─────────
    @Transactional
    public void submitAnswer(AnswerRequest request) {
        UUID userId = getCurrentUserId();
        User user   = getCurrentUser();

        UUID flashcardId = UUID.fromString(request.getFlashcardId());
        FlashCard flashcard = flashCardRepository.findById(flashcardId)
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_NOT_EXISTED));

        boolean correct = determineCorrect(request);

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

        // Cập nhật đúng/sai cho QuizSession nếu có sessionId
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            try {
                UUID sessionId = UUID.fromString(request.getSessionId());
                quizSessionRepository.findById(sessionId).ifPresent(qs -> {
                    if (correct) qs.setCorrectCount(qs.getCorrectCount() + 1);
                    quizSessionRepository.save(qs);
                });
            } catch (Exception ignored) {}
        }
    }

    // ── UC-STUDY-06: End session + cập nhật streak ────────────────────
    @Transactional
    public SessionResultResponse endSession(String sessionId, List<String> wrongTerms) {
        User user = getCurrentUser();

        // Cập nhật streak
        int[] streakInfo = updateStreak(user);
        userRepository.save(user);

        // Lấy thông tin QuizSession
        QuizSession session = null;
        if (sessionId != null && !sessionId.isEmpty()) {
            try {
                session = quizSessionRepository.findById(UUID.fromString(sessionId)).orElse(null);
            } catch (Exception ignored) {}
        }

        int correctCount    = 0;
        int totalQuestions  = 0;
        int durationSeconds = 0;

        if (session != null) {
            session.setEndedAt(LocalDateTime.now());
            correctCount   = session.getCorrectCount() != null ? session.getCorrectCount() : 0;
            totalQuestions = session.getTotalQuestions() != null ? session.getTotalQuestions() : 0;
            if (session.getStartedAt() != null) {
                durationSeconds = (int) ChronoUnit.SECONDS.between(session.getStartedAt(), LocalDateTime.now());
            }
            quizSessionRepository.save(session);
        }

        List<String> wrongs = wrongTerms != null ? wrongTerms : new ArrayList<>();

        return SessionResultResponse.builder()
                .sessionId(sessionId)
                .mode(session != null ? session.getMode() : "")
                .correctCount(correctCount)
                .totalQuestions(totalQuestions)
                .durationSeconds(durationSeconds)
                .currentStreak(streakInfo[0])
                .newRecord(streakInfo[1] == 1)
                .wrongTerms(wrongs)
                .build();
    }

    // ── UC-STAT-01,02,03: Stat Summary ───────────────────────────────
    public StatSummaryResponse getStatSummary() {
        UUID userId = getCurrentUserId();
        User user   = getCurrentUser();

        long wordsLearned  = studyRecordRepository.countByUserId(userId);
        long wordsMastered = studyRecordRepository.countByUserIdAndRememberedTrue(userId);

        Object[] accuracy = studyRecordRepository.sumAccuracy(userId);
        long totalAnswers = 0;
        long wrongAnswers = 0;
        if (accuracy != null && accuracy[0] != null) {
            totalAnswers = ((Number) accuracy[0]).longValue() + (accuracy[1] != null ? ((Number) accuracy[1]).longValue() : 0);
            wrongAnswers = accuracy[1] != null ? ((Number) accuracy[1]).longValue() : 0;
        }
        double accuracyPct = totalAnswers == 0 ? 0.0
                : (totalAnswers - wrongAnswers) * 100.0 / totalAnswers;

        int currentStreak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
        int longestStreak = user.getLongestStreak() != null ? user.getLongestStreak() : 0;
        boolean isNewRecord = currentStreak >= longestStreak && currentStreak > 0;

        return StatSummaryResponse.builder()
                .wordsLearned((int) wordsLearned)
                .wordsMastered((int) wordsMastered)
                .totalAnswers(totalAnswers)
                .wrongAnswers(wrongAnswers)
                .accuracyPercent(accuracyPct)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .newRecord(isNewRecord)
                .build();
    }

    // ── UC-STAT-04,05: Study Activity chart ──────────────────────────
    public List<DayActivityResponse> getStudyActivity(String period) {
        UUID userId = getCurrentUserId();
        LocalDateTime from = "monthly".equals(period)
                ? LocalDateTime.now().minusDays(30)
                : LocalDateTime.now().minusDays(7);

        List<QuizSession> sessions = quizSessionRepository.findCompletedSessions(userId, from);

        // Group by date
        Map<String, List<QuizSession>> byDate = sessions.stream()
                .collect(Collectors.groupingBy(qs ->
                        qs.getStartedAt().toLocalDate().format(DateTimeFormatter.ISO_DATE)));

        List<DayActivityResponse> result = new ArrayList<>();
        LocalDate start = from.toLocalDate();
        LocalDate end   = LocalDate.now();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            String key = d.format(DateTimeFormatter.ISO_DATE);
            List<QuizSession> daySessions = byDate.getOrDefault(key, List.of());
            int wordCount = daySessions.stream()
                    .mapToInt(qs -> qs.getTotalQuestions() != null ? qs.getTotalQuestions() : 0).sum();
            int correct   = daySessions.stream()
                    .mapToInt(qs -> qs.getCorrectCount()    != null ? qs.getCorrectCount()    : 0).sum();
            double acc = wordCount == 0 ? 0.0 : correct * 100.0 / wordCount;
            result.add(DayActivityResponse.builder()
                    .date(key).wordCount(wordCount).accuracyPercent(acc).build());
        }
        return result;
    }

    // ── UC-STAT-06: Set Progress ──────────────────────────────────────
    public List<SetProgressResponse> getSetProgress() {
        UUID userId = getCurrentUserId();

        List<FlashCardSet> sets = flashCardSetRepository.findAllByUserIdAndDeletedFalse(userId);

        return sets.stream().map(set -> {
            List<FlashCard> cards = flashCardRepository
                    .findByFlashCardSetIdAndDeletedFalse(set.getId());
            int total = cards.size();

            List<StudyRecord> records = cards.stream()
                    .map(fc -> studyRecordRepository
                            .findByUserIdAndFlashcardId(userId, fc.getId()).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            int remembered   = (int) records.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getRemembered())).count();
            int notYet       = records.size() - remembered;
            int notStudied   = total - records.size();
            double pct = total == 0 ? 0.0 : remembered * 100.0 / total;

            return SetProgressResponse.builder()
                    .setId(set.getId().toString())
                    .setName(set.getName())
                    .totalWords(total)
                    .rememberedCount(remembered)
                    .notYetCount(notYet)
                    .notStudiedCount(notStudied)
                    .percentage(pct)
                    .build();
        }).collect(Collectors.toList());
    }

    // ── SM-2 Algorithm ───────────────────────────────────────────────
    private void applySM2(StudyRecord record, boolean correct) {
        int quality = correct ? 4 : 0;

        float ef  = record.getEaseFactor()     != null ? record.getEaseFactor()     : 2.5f;
        int interval = record.getIntervalDays() != null ? record.getIntervalDays() : 1;
        int rep      = record.getRepetitionCount() != null ? record.getRepetitionCount() : 0;

        if (quality < 3) {
            rep      = 0;
            interval = 1;
        } else {
            ef += 0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f);
            ef  = Math.max(1.3f, ef);
            if (rep == 0)      interval = 1;
            else if (rep == 1) interval = 6;
            else               interval = Math.round(interval * ef);
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
    private int[] updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate last  = user.getLastStudyDate();
        if (last == null)
            user.setCurrentStreak(1);
        else if (last.equals(today))
            { /* đã học hôm nay rồi, không tăng */ }
        else if (last.equals(today.minusDays(1)))
            user.setCurrentStreak((user.getCurrentStreak() != null ? user.getCurrentStreak() : 0) + 1);
        else
            user.setCurrentStreak(1);
        user.setLastStudyDate(today);

        int isNew = 0;
        int current  = user.getCurrentStreak() != null ? user.getCurrentStreak() : 1;
        int longest  = user.getLongestStreak()  != null ? user.getLongestStreak()  : 0;
        if (current > longest) {
            user.setLongestStreak(current);
            isNew = 1;
        }
        return new int[]{current, isNew};
    }

    private boolean determineCorrect(AnswerRequest req) {
        // flashcard mode dùng remembered, quiz mode dùng correct
        if (Boolean.TRUE.equals(req.getRemembered())) return true;
        if (Boolean.FALSE.equals(req.getRemembered()) && req.getCorrect() == null) return false;
        return Boolean.TRUE.equals(req.getCorrect());
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
                List<String> pool = new ArrayList<>(allDefs);
                pool.removeIf(d -> d.equals(fc.getDefinition()));
                Collections.shuffle(pool);
                distractors = pool.stream().limit(3).collect(Collectors.toList());
            }
            return StartSessionResponse.SessionCardResponse.builder()
                    .flashcardId(fc.getId().toString())
                    .term(fc.getTerm())
                    .definition(fc.getDefinition())
                    .ipa(fc.getIpa())
                    .audioUrl(null)
                    .image(fc.getImage())
                    .example(fc.getExample())
                    .distractors(distractors)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<StudySetItemResponse> buildDueItems(Map<UUID, List<StudyRecord>> map) {
        return map.entrySet().stream().map(e -> {
            List<StudyRecord> records = e.getValue();
            FlashCardSet set = records.get(0).getFlashcard().getFlashCardSet();
            List<String> preview = records.stream().limit(3)
                    .map(sr -> sr.getFlashcard().getTerm()).collect(Collectors.toList());
            long rem  = records.stream().filter(sr -> Boolean.TRUE.equals(sr.getRemembered())).count();
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
        return getCurrentUser().getId();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}

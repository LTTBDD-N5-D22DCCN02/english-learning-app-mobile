package com.estudy.backend.configuration;

import com.estudy.backend.entity.*;
import com.estudy.backend.enums.Privacy;
import com.estudy.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final FlashCardSetRepository flashCardSetRepository;
    private final FlashCardRepository flashCardRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (userRepository.count() > 0) {
                log.info("Data already seeded, skipping.");
                return;
            }

            log.info("Seeding test data...");
        } catch (Exception e) {
            log.error("Error checking if data seeded, proceeding anyway", e);
        }

        // ── 1. User test ──────────────────────────────────────
        User user = User.builder()
                .username("maihuong")
                .email("huong@gmail.com")
                .password(passwordEncoder.encode("123456789"))
                .fullName("Tran Mai Huong")
                .dob(LocalDate.of(2004, 1, 21))
                .phone("0987654321")
                .currentStreak(5)
                .longestStreak(12)
                .deleted(false)
                .build();
        try {
            user = userRepository.save(user);
            log.info("Created user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to create user", e);
            return;
        }

        // ── 2. TOEIC Vocabulary (10 từ) ───────────────────────
        FlashCardSet toeicSet = FlashCardSet.builder()
                .name("TOEIC Vocabulary")
                .description("Từ vựng TOEIC thông dụng nhất")
                .privacy(Privacy.PUBLIC)
                .user(user)
                .deleted(false)
                .build();
        try {
            toeicSet = flashCardSetRepository.save(toeicSet);
            log.info("Created flashcard set: {}", toeicSet.getName());
        } catch (Exception e) {
            log.error("Failed to create TOEIC flashcard set", e);
            return;
        }

        // Save từng từ riêng — tránh lỗi với immutable List.of()
        List<FlashCard> toeicCards = new ArrayList<>();
        toeicCards.add(card("abundant",    "dồi dào, phong phú",    "/əˈbʌndənt/",   toeicSet));
        toeicCards.add(card("allocate",    "phân bổ, phân chia",     "/ˈæləkeɪt/",    toeicSet));
        toeicCards.add(card("assess",      "đánh giá, nhận xét",     "/əˈsɛs/",        toeicSet));
        toeicCards.add(card("revenue",     "doanh thu",              "/ˈrɛvənjuː/",    toeicSet));
        toeicCards.add(card("implement",   "triển khai, thực hiện",  "/ˈɪmplɪmɛnt/",   toeicSet));
        toeicCards.add(card("collaborate", "hợp tác, cộng tác",      "/kəˈlæbəreɪt/",  toeicSet));
        toeicCards.add(card("efficient",   "hiệu quả, có năng suất", "/ɪˈfɪʃənt/",     toeicSet));
        toeicCards.add(card("negotiate",   "đàm phán, thương lượng", "/nɪˈɡoʊʃieɪt/",  toeicSet));
        toeicCards.add(card("mandatory",   "bắt buộc, cưỡng chế",   "/ˈmændətɔːri/",  toeicSet));
        toeicCards.add(card("pursuant",    "theo, chiếu theo",       "/pərˈsuːənt/",   toeicSet));
        try {
            flashCardRepository.saveAll(toeicCards);
            log.info("Saved TOEIC set with {} cards", toeicCards.size());
        } catch (Exception e) {
            log.error("Failed to save TOEIC flashcards", e);
            return;
        }

        // ── 3. Business English (7 từ) ────────────────────────
        FlashCardSet bizSet = FlashCardSet.builder()
                .name("Business English")
                .description("Tiếng Anh thương mại")
                .privacy(Privacy.PUBLIC)
                .user(user)
                .deleted(false)
                .build();
        try {
            bizSet = flashCardSetRepository.save(bizSet);
            log.info("Created flashcard set: {}", bizSet.getName());
        } catch (Exception e) {
            log.error("Failed to create Business English flashcard set", e);
            return;
        }

        List<FlashCard> bizCards = new ArrayList<>();
        bizCards.add(card("agenda",    "chương trình nghị sự",   "/əˈdʒɛndə/",    bizSet));
        bizCards.add(card("deadline",  "thời hạn cuối",           "/ˈdɛdlaɪn/",    bizSet));
        bizCards.add(card("invoice",   "hóa đơn",                 "/ˈɪnvɔɪs/",     bizSet));
        bizCards.add(card("merger",    "sự sáp nhập",             "/ˈmɜːrdʒər/",   bizSet));
        bizCards.add(card("benchmark", "tiêu chuẩn so sánh",      "/ˈbɛntʃmɑːrk/", bizSet));
        bizCards.add(card("leverage",  "đòn bẩy, tận dụng",       "/ˈlɛvərɪdʒ/",   bizSet));
        bizCards.add(card("outsource", "thuê ngoài",              "/ˈaʊtsɔːrs/",   bizSet));
        try {
            flashCardRepository.saveAll(bizCards);
            log.info("Saved Business English set with {} cards", bizCards.size());
        } catch (Exception e) {
            log.error("Failed to save Business English flashcards", e);
            return;
        }

        log.info("Seeded: user maihuong/123456789, 2 sets, {} cards total.",
                toeicCards.size() + bizCards.size());
    }

    private FlashCard card(String term, String definition, String ipa, FlashCardSet set) {
        return FlashCard.builder()
                .term(term)
                .definition(definition)
                .ipa(ipa)
                .deleted(false)
                .flashCardSet(set)
                .build();
    }
}
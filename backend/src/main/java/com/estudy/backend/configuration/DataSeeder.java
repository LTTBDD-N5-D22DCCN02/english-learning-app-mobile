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
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final FlashCardSetRepository flashCardSetRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Data already seeded, skipping.");
            return;
        }

        log.info("Seeding test data...");

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
        userRepository.save(user);

        // ── 2. TOEIC Vocabulary ───────────────────────────────
        FlashCardSet toeicSet = FlashCardSet.builder()
                .name("TOEIC Vocabulary")
                .description("Từ vựng TOEIC thông dụng nhất")
                .privacy(Privacy.PUBLIC)
                .user(user)
                .deleted(false)
                .build();

        toeicSet.setFlashCards(List.of(
                card("abundant",    "dồi dào, phong phú",    "/əˈbʌndənt/",   toeicSet),
                card("allocate",    "phân bổ, phân chia",     "/ˈæləkeɪt/",    toeicSet),
                card("assess",      "đánh giá, nhận xét",     "/əˈsɛs/",        toeicSet),
                card("revenue",     "doanh thu",              "/ˈrɛvənjuː/",    toeicSet),
                card("implement",   "triển khai, thực hiện",  "/ˈɪmplɪmɛnt/",   toeicSet),
                card("collaborate", "hợp tác, cộng tác",      "/kəˈlæbəreɪt/",  toeicSet),
                card("efficient",   "hiệu quả, có năng suất", "/ɪˈfɪʃənt/",     toeicSet),
                card("negotiate",   "đàm phán, thương lượng", "/nɪˈɡoʊʃieɪt/",  toeicSet),
                card("mandatory",   "bắt buộc, cưỡng chế",   "/ˈmændətɔːri/",  toeicSet),
                card("pursuant",    "theo, chiếu theo",       "/pərˈsuːənt/",   toeicSet)
        ));
        flashCardSetRepository.save(toeicSet);

        // ── 3. Business English ───────────────────────────────
        FlashCardSet bizSet = FlashCardSet.builder()
                .name("Business English")
                .description("Tiếng Anh thương mại")
                .privacy(Privacy.PUBLIC)
                .user(user)
                .deleted(false)
                .build();

        bizSet.setFlashCards(List.of(
                card("agenda",    "chương trình nghị sự",   "/əˈdʒɛndə/",    bizSet),
                card("deadline",  "thời hạn cuối",           "/ˈdɛdlaɪn/",    bizSet),
                card("invoice",   "hóa đơn",                 "/ˈɪnvɔɪs/",     bizSet),
                card("merger",    "sự sáp nhập",             "/ˈmɜːrdʒər/",   bizSet),
                card("benchmark", "tiêu chuẩn so sánh",      "/ˈbɛntʃmɑːrk/", bizSet),
                card("leverage",  "đòn bẩy, tận dụng",       "/ˈlɛvərɪdʒ/",   bizSet),
                card("outsource", "thuê ngoài",              "/ˈaʊtsɔːrs/",   bizSet)
        ));
        flashCardSetRepository.save(bizSet);

        log.info("Seeded: user maihuong/123456789, 2 sets, 17 cards.");
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
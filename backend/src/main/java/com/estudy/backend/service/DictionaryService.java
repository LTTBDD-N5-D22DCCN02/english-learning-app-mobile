package com.estudy.backend.service;

import com.estudy.backend.dto.response.SuggestResponse;
import com.estudy.backend.dto.response.SuggestResponse.Meaning;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DictionaryService {

    private static final String API_URL =
            "https://api.dictionaryapi.dev/api/v2/entries/en/";

    private final RestTemplate restTemplate;

    /**
     * Tra từ điển, trả về TẤT CẢ các nghĩa (mỗi từ loại = 1 Meaning).
     * Mỗi Meaning bao gồm: partOfSpeech, definition, example, synonyms.
     */
    @SuppressWarnings("unchecked")
    public SuggestResponse lookup(String term) {
        try {
            String url = API_URL + term.trim().toLowerCase();
            List<Map<String, Object>> entries =
                    restTemplate.getForObject(url, List.class);

            if (entries == null || entries.isEmpty()) {
                return SuggestResponse.builder()
                        .term(term)
                        .meanings(List.of())
                        .build();
            }

            Map<String, Object> entry = entries.get(0);

            // ── IPA ──────────────────────────────────────────────────────────
            String ipa = extractIpa(entry);

            // ── Meanings ─────────────────────────────────────────────────────
            List<Meaning> meanings = new ArrayList<>();

            List<Map<String, Object>> rawMeanings =
                    (List<Map<String, Object>>) entry.get("meanings");

            if (rawMeanings != null) {
                for (Map<String, Object> rawMeaning : rawMeanings) {
                    String partOfSpeech = (String) rawMeaning.get("partOfSpeech");

                    List<Map<String, Object>> defs =
                            (List<Map<String, Object>>) rawMeaning.get("definitions");

                    if (defs == null) continue;

                    // Lấy tất cả định nghĩa của từng từ loại
                    for (Map<String, Object> def : defs) {
                        String definition = (String) def.get("definition");
                        String example    = (String) def.get("example");

                        // Synonyms trong từng definition
                        List<String> synonyms = new ArrayList<>();
                        List<String> defSynonyms = (List<String>) def.get("synonyms");
                        if (defSynonyms != null) synonyms.addAll(defSynonyms);

                        if (definition == null || definition.isBlank()) continue;

                        meanings.add(Meaning.builder()
                                .partOfSpeech(partOfSpeech)
                                .definition(definition)
                                .example(example)
                                .build());
                    }
                }
            }

            return SuggestResponse.builder()
                    .term(term)
                    .ipa(ipa)
                    .meanings(meanings)
                    .build();

        } catch (Exception e) {
            log.warn("Dictionary lookup failed for '{}': {}", term, e.getMessage());
            return SuggestResponse.builder()
                    .term(term)
                    .meanings(List.of())
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractIpa(Map<String, Object> entry) {
        try {
            List<Map<String, Object>> phonetics =
                    (List<Map<String, Object>>) entry.get("phonetics");
            if (phonetics == null) return null;
            return phonetics.stream()
                    .filter(p -> p.get("text") != null)
                    .map(p -> (String) p.get("text"))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
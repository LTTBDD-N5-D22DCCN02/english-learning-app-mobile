package com.estudy.backend.service;

import com.estudy.backend.dto.response.SuggestResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DictionaryService {

    static String DICTIONARY_API = "https://api.dictionaryapi.dev/api/v2/entries/en/";

    RestTemplate restTemplate;

    /**
     * Calls the Free Dictionary API to look up a term.
     * Returns definition, IPA, and example if found.
     */
    @SuppressWarnings("unchecked")
    public SuggestResponse lookup(String term) {
        try {
            String url = DICTIONARY_API + term.trim().toLowerCase();
            List<Map<String, Object>> entries = restTemplate.getForObject(url, List.class);

            if (entries == null || entries.isEmpty()) {
                return SuggestResponse.builder().term(term).build();
            }

            Map<String, Object> entry = entries.get(0);

            // Extract IPA
            String ipa = null;
            List<Map<String, Object>> phonetics = (List<Map<String, Object>>) entry.get("phonetics");
            if (phonetics != null) {
                ipa = phonetics.stream()
                        .filter(p -> p.get("text") != null)
                        .map(p -> (String) p.get("text"))
                        .findFirst().orElse(null);
            }

            // Extract definition and example from first meaning
            String definition = null;
            String example = null;
            List<Map<String, Object>> meanings = (List<Map<String, Object>>) entry.get("meanings");
            if (meanings != null && !meanings.isEmpty()) {
                Map<String, Object> meaning = meanings.get(0);
                String partOfSpeech = (String) meaning.get("partOfSpeech");

                List<Map<String, Object>> defs = (List<Map<String, Object>>) meaning.get("definitions");
                if (defs != null && !defs.isEmpty()) {
                    Map<String, Object> firstDef = defs.get(0);
                    String rawDef = (String) firstDef.get("definition");

                    // Prepend part of speech in parentheses
                    if (partOfSpeech != null && rawDef != null) {
                        definition = "(" + partOfSpeech + ") " + rawDef;
                    } else {
                        definition = rawDef;
                    }
                    example = (String) firstDef.get("example");
                }
            }

            return SuggestResponse.builder()
                    .term(term)
                    .ipa(ipa)
                    .definition(definition)
                    .example(example)
                    .build();

        } catch (Exception e) {
            log.warn("Dictionary lookup failed for term '{}': {}", term, e.getMessage());
            return SuggestResponse.builder().term(term).build();
        }
    }
}
package com.estudy.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SuggestResponse {

    private String term;
    private String ipa;

    /** Danh sách các nghĩa — mỗi nghĩa thuộc 1 từ loại khác nhau */
    private List<Meaning> meanings;

    @Data
    @Builder
    public static class Meaning {
        /** Từ loại: noun, verb, adjective... */
        private String partOfSpeech;

        /** Định nghĩa đầy đủ */
        private String definition;

        /** Câu ví dụ (có thể null) */
        private String example;

    }
}
package com.estudy.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SuggestResponse {
    private String term;
    private String definition;
    private String ipa;
    private String example;
}
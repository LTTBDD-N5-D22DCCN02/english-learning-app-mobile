package com.estudy.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DayActivityResponse {

    /** Format: "2026-04-01" */
    String date;

    /** UC-STAT-04: số từ học trong ngày */
    int wordCount;

    /** UC-STAT-05: tỉ lệ đúng trong ngày (0-100) */
    double accuracyPercent;
}
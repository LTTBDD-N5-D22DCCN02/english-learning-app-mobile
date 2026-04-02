package com.estudy.backend.controller;

import com.estudy.backend.dto.response.ApiResponse;
import com.estudy.backend.dto.response.StudyTodayResponse;
import com.estudy.backend.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    // GET /study/today
    @GetMapping("/today")
    public ApiResponse<StudyTodayResponse> getStudyToday() {
        return ApiResponse.<StudyTodayResponse>builder()
                .result(studyService.getStudyToday())
                .build();
    }
}
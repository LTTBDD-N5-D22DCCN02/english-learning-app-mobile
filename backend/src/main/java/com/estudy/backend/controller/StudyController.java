package com.estudy.backend.controller;

import com.estudy.backend.dto.request.AnswerRequest;
import com.estudy.backend.dto.response.ApiResponse;
import com.estudy.backend.dto.response.StudyTodayResponse;
import com.estudy.backend.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    // POST /study/answer — ghi nhận kết quả + cập nhật SM-2
    @PostMapping("/answer")
    public ApiResponse<Void> submitAnswer(@RequestBody AnswerRequest request) {
        studyService.submitAnswer(request);
        return ApiResponse.<Void>builder().build();
    }
}

package com.estudy.backend.controller;

import com.estudy.backend.dto.request.AnswerRequest;
import com.estudy.backend.dto.request.StartSessionRequest;
import com.estudy.backend.dto.response.*;
import com.estudy.backend.service.StudyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    /** GET /study/today — UC-STUDY-01 */
    @GetMapping("/today")
    public ApiResponse<StudyTodayResponse> getStudyToday() {
        return ApiResponse.<StudyTodayResponse>builder()
                .result(studyService.getStudyToday()).build();
    }

    /** POST /study/session/start — UC-STUDY-02~05 */
    @PostMapping("/session/start")
    public ApiResponse<StartSessionResponse> startSession(
            @Valid @RequestBody StartSessionRequest req) {
        return ApiResponse.<StartSessionResponse>builder()
                .result(studyService.startSession(req)).build();
    }

    /** POST /study/answer — UC-STUDY-02~05: ghi nhận câu trả lời + cập nhật SM-2 */
    @PostMapping("/answer")
    public ApiResponse<Void> submitAnswer(@Valid @RequestBody AnswerRequest req) {
        studyService.submitAnswer(req);
        return ApiResponse.<Void>builder().build();
    }

    /** PUT /study/session/{id}/end — UC-STUDY-06: kết thúc phiên + cập nhật streak */
    @PutMapping("/session/{sessionId}/end")
    public ApiResponse<SessionResultResponse> endSession(
            @PathVariable String sessionId,
            @RequestBody(required = false) List<String> wrongTerms) {
        return ApiResponse.<SessionResultResponse>builder()
                .result(studyService.endSession(sessionId, wrongTerms)).build();
    }

    /** GET /study/stats/summary — UC-STAT-01,02,03 */
    @GetMapping("/stats/summary")
    public ApiResponse<StatSummaryResponse> getStatSummary() {
        return ApiResponse.<StatSummaryResponse>builder()
                .result(studyService.getStatSummary()).build();
    }

    /** GET /study/stats/activity?period=weekly|monthly — UC-STAT-04,05 */
    @GetMapping("/stats/activity")
    public ApiResponse<List<DayActivityResponse>> getStudyActivity(
            @RequestParam(defaultValue = "weekly") String period) {
        return ApiResponse.<List<DayActivityResponse>>builder()
                .result(studyService.getStudyActivity(period)).build();
    }

    /** GET /study/stats/sets — UC-STAT-06 */
    @GetMapping("/stats/sets")
    public ApiResponse<List<SetProgressResponse>> getSetProgress() {
        return ApiResponse.<List<SetProgressResponse>>builder()
                .result(studyService.getSetProgress()).build();
    }
}

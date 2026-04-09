package com.estudy.backend.controller;

import com.estudy.backend.dto.request.ClassFlashCardSetRequest;
import com.estudy.backend.dto.request.UpdateMemberRoleRequest;
import com.estudy.backend.dto.request.ClassRequest;
import com.estudy.backend.dto.request.CopyClassRequest;
import com.estudy.backend.dto.request.JoinClassRequest;
import com.estudy.backend.dto.response.ApiResponse;
import com.estudy.backend.dto.response.ClassMemberResponse;
import com.estudy.backend.dto.response.ClassResponse;
import com.estudy.backend.dto.response.FlashCardSetResponse;
import com.estudy.backend.service.ClassService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassController {

    ClassService classService;

    // ─── UC-01: Tạo lớp học ────────────────────────────────────────
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ClassResponse> createClass(@RequestBody @Valid ClassRequest request) {
        return ApiResponse.<ClassResponse>builder()
                .result(classService.createClass(request))
                .build();
    }

    // ─── UC-02: Sửa thông tin lớp học ─────────────────────────────
    @PutMapping("/{classId}")
    ApiResponse<ClassResponse> updateClass(
            @PathVariable UUID classId,
            @RequestBody @Valid ClassRequest request) {
        return ApiResponse.<ClassResponse>builder()
                .result(classService.updateClass(classId, request))
                .build();
    }

    // ─── UC-03: Xóa lớp học ────────────────────────────────────────
    @DeleteMapping("/{classId}")
    ApiResponse<Void> deleteClass(@PathVariable UUID classId) {
        classService.deleteClass(classId);
        return ApiResponse.<Void>builder()
                .message("Class deleted successfully")
                .build();
    }

    // ─── UC-04: Xem danh sách lớp học đã tham gia ─────────────────
    @GetMapping("/my")
    ApiResponse<List<ClassResponse>> getMyClasses() {
        return ApiResponse.<List<ClassResponse>>builder()
                .result(classService.getMyClasses())
                .build();
    }

    // ─── UC-05: Tham gia lớp học bằng mã lớp ──────────────────────
    @PostMapping("/join")
    ApiResponse<Void> joinClass(@RequestBody @Valid JoinClassRequest request) {
        classService.joinClass(request);
        return ApiResponse.<Void>builder()
                .message("Join request sent. Please wait for leader approval.")
                .build();
    }

    // ─── UC-06: Rời khỏi lớp học ───────────────────────────────────
    @DeleteMapping("/{classId}/leave")
    ApiResponse<Void> leaveClass(@PathVariable UUID classId) {
        classService.leaveClass(classId);
        return ApiResponse.<Void>builder()
                .message("Left class successfully")
                .build();
    }

    // ─── UC-07 + UC-08: Xem chi tiết lớp học ──────────────────────
    @GetMapping("/{classId}")
    ApiResponse<ClassResponse> getClassDetail(@PathVariable UUID classId) {
        return ApiResponse.<ClassResponse>builder()
                .result(classService.getClassDetail(classId))
                .build();
    }

    // ─── UC-09: Sao chép lớp học ───────────────────────────────────
    @PostMapping("/{classId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ClassResponse> copyClass(
            @PathVariable UUID classId,
            @RequestBody @Valid CopyClassRequest request) {
        return ApiResponse.<ClassResponse>builder()
                .result(classService.copyClass(classId, request))
                .build();
    }

    // ─── UC-10: Tìm kiếm lớp học ───────────────────────────────────
    @GetMapping("/search")
    ApiResponse<List<ClassResponse>> searchMyClasses(
            @RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.<List<ClassResponse>>builder()
                .result(classService.searchMyClasses(keyword))
                .build();
    }

    // ─── Group Management: Danh sách thành viên ────────────────────
    @GetMapping("/{classId}/members")
    ApiResponse<List<ClassMemberResponse>> getMembers(@PathVariable UUID classId) {
        return ApiResponse.<List<ClassMemberResponse>>builder()
                .result(classService.getMembers(classId))
                .build();
    }

    // ─── Group Management: Danh sách yêu cầu chờ duyệt ───────────
    @GetMapping("/{classId}/members/pending")
    ApiResponse<List<ClassMemberResponse>> getPendingRequests(@PathVariable UUID classId) {
        return ApiResponse.<List<ClassMemberResponse>>builder()
                .result(classService.getPendingRequests(classId))
                .build();
    }

    // ─── Group Management: Duyệt yêu cầu ─────────────────────────
    @PatchMapping("/{classId}/members/{memberId}/approve")
    ApiResponse<Void> approveRequest(
            @PathVariable UUID classId,
            @PathVariable UUID memberId) {
        classService.approveRequest(classId, memberId);
        return ApiResponse.<Void>builder()
                .message("Request approved successfully")
                .build();
    }

    // ─── Group Management: Từ chối yêu cầu ────────────────────────
    @PatchMapping("/{classId}/members/{memberId}/reject")
    ApiResponse<Void> rejectRequest(
            @PathVariable UUID classId,
            @PathVariable UUID memberId) {
        classService.rejectRequest(classId, memberId);
        return ApiResponse.<Void>builder()
                .message("Request rejected")
                .build();
    }

    // ─── Group Management: Xóa thành viên ─────────────────────────
    @DeleteMapping("/{classId}/members/{userId}")
    ApiResponse<Void> removeMember(
            @PathVariable UUID classId,
            @PathVariable UUID userId) {
        classService.removeMember(classId, userId);
        return ApiResponse.<Void>builder()
                .message("Member removed successfully")
                .build();
    }

    @PostMapping("/{classId}/join-public")
    ApiResponse<Void> joinPublicClass(@PathVariable UUID classId) {
        classService.joinPublicClass(classId);
        return ApiResponse.<Void>builder()
                .message("Join request sent. Please wait for leader approval.")
                .build();
    }

    // UC-12: Xem danh sách lớp học công khai (Discover)
    @GetMapping("/public")
    ApiResponse<List<ClassResponse>> getPublicClasses(
            @RequestParam(required = false, defaultValue = "") String keyword) {

        return ApiResponse.<List<ClassResponse>>builder()
                .result(classService.getPublicClasses(keyword))
                .build();
    }

    // ─── UC-16: Lấy mã lớp ─────────────────────────────────────
    @GetMapping("/{classId}/code")
    ApiResponse<String> getClassCode(@PathVariable UUID classId) {
        return ApiResponse.<String>builder()
                .result(classService.getClassCode(classId))
                .build();
    }

    // ─── UC-23: Tìm kiếm thành viên ────────────────────────────
    @GetMapping("/{classId}/members/search")
    ApiResponse<List<ClassMemberResponse>> searchMembers(
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.<List<ClassMemberResponse>>builder()
                .result(classService.searchMembers(classId, keyword))
                .build();
    }

    // ─── UC-24: Cập nhật quyền thành viên ──────────────────────
    @PatchMapping("/{classId}/members/{userId}/role")
    ApiResponse<Void> updateMemberRole(
            @PathVariable UUID classId,
            @PathVariable UUID userId,
            @RequestBody @Valid UpdateMemberRoleRequest request) {
        classService.updateMemberRole(classId, userId, request.getRole());
        return ApiResponse.<Void>builder()
                .message("Member role updated successfully")
                .build();
    }

    // ─── UC-17: Xem danh sách bộ Flashcard trong lớp ───────────
    @GetMapping("/{classId}/flashcard-sets")
    ApiResponse<List<FlashCardSetResponse>> getClassFlashCardSets(@PathVariable UUID classId) {
        return ApiResponse.<List<FlashCardSetResponse>>builder()
                .result(classService.getClassFlashCardSets(classId, null))
                .build();
    }

    // ─── UC-22: Tìm kiếm bộ Flashcard trong lớp ────────────────
    @GetMapping("/{classId}/flashcard-sets/search")
    ApiResponse<List<FlashCardSetResponse>> searchClassFlashCardSets(
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.<List<FlashCardSetResponse>>builder()
                .result(classService.getClassFlashCardSets(classId, keyword))
                .build();
    }

    // ─── UC-18: Thêm Flashcard Set vào lớp ─────────────────────
    @PostMapping("/{classId}/flashcard-sets")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<FlashCardSetResponse> addClassFlashCardSet(
            @PathVariable UUID classId,
            @RequestBody @Valid ClassFlashCardSetRequest request) {
        return ApiResponse.<FlashCardSetResponse>builder()
                .result(classService.addClassFlashCardSet(classId, request))
                .build();
    }

    // ─── UC-19: Sửa Flashcard Set trong lớp ────────────────────
    @PutMapping("/{classId}/flashcard-sets/{setId}")
    ApiResponse<FlashCardSetResponse> updateClassFlashCardSet(
            @PathVariable UUID classId,
            @PathVariable UUID setId,
            @RequestBody @Valid ClassFlashCardSetRequest request) {
        return ApiResponse.<FlashCardSetResponse>builder()
                .result(classService.updateClassFlashCardSet(classId, setId, request))
                .build();
    }

    // ─── UC-20: Xóa Flashcard Set khỏi lớp ─────────────────────
    @DeleteMapping("/{classId}/flashcard-sets/{setId}")
    ApiResponse<Void> deleteClassFlashCardSet(
            @PathVariable UUID classId,
            @PathVariable UUID setId) {
        classService.deleteClassFlashCardSet(classId, setId);
        return ApiResponse.<Void>builder()
                .message("Flashcard set removed from class successfully")
                .build();
    }
}
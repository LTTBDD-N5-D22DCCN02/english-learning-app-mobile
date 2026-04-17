package com.estudy.backend.controller;

import com.estudy.backend.dto.response.ApiResponse;
import com.estudy.backend.dto.response.NotificationResponse;
import com.estudy.backend.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    NotificationService notificationService;

    // ─── UC-01: Lấy danh sách thông báo (sắp xếp mới nhất trước) ────
    @GetMapping
    ApiResponse<List<NotificationResponse>> getNotifications() {
        return ApiResponse.<List<NotificationResponse>>builder()
                .result(notificationService.getNotifications())
                .build();
    }

    // ─── UC-01 (step 5): Đánh dấu thông báo đã đọc ──────────────────
    @PatchMapping("/{id}/read")
    ApiResponse<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ApiResponse.<Void>builder()
                .message("Notification marked as read")
                .build();
    }

    // ─── Đếm số thông báo chưa đọc (dùng cho badge) ─────────────────
    @GetMapping("/unread-count")
    ApiResponse<Long> getUnreadCount() {
        return ApiResponse.<Long>builder()
                .result(notificationService.getUnreadCount())
                .build();
    }
}

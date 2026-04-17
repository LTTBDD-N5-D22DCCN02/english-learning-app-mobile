package com.estudy.backend.service;

import com.estudy.backend.dto.response.NotificationMetadata;
import com.estudy.backend.dto.response.NotificationResponse;
import com.estudy.backend.entity.Notification;
import com.estudy.backend.entity.User;
import com.estudy.backend.exception.AppException;
import com.estudy.backend.exception.ErrorCode;
import com.estudy.backend.repository.NotificationRepository;
import com.estudy.backend.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {

    NotificationRepository notificationRepository;
    UserRepository         userRepository;

    // ─── Helper: lấy user hiện tại ───────────────────────────────────
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // ─── UC-01: Lấy danh sách thông báo ─────────────────────────────
    public List<NotificationResponse> getNotifications() {
        User currentUser = getCurrentUser();
        return notificationRepository
                .findByUserOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Đếm số thông báo chưa đọc ──────────────────────────────────
    public long getUnreadCount() {
        return notificationRepository.countByUserAndIsReadFalse(getCurrentUser());
    }

    // ─── UC-01 (step 5): Đánh dấu đã đọc ────────────────────────────
    @Transactional
    public void markAsRead(UUID id) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        User currentUser = getCurrentUser();
        if (!notif.getUser().getId().equals(currentUser.getId()))
            throw new AppException(ErrorCode.UNAUTHORIZED);

        notif.setRead(true);
        notificationRepository.save(notif);
    }

    // ─── Mapping entity → DTO ─────────────────────────────────────────
    private NotificationResponse toResponse(Notification n) {
        String rawType = n.getType() != null ? n.getType() : "";

        String feType   = mapFeType(rawType);
        String title    = mapTitle(rawType);
        NotificationMetadata metadata = buildMetadata(rawType, n.getIdRef());

        return NotificationResponse.builder()
                .id(n.getId().toString())
                .type(feType)
                .title(title)
                .content(n.getContent())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null)
                .metadata(metadata)
                .build();
    }

    /**
     * Map raw DB type → FE type
     *   JOIN_REQUEST  → join_request
     *   JOIN_APPROVED → join_request
     *   JOIN_REJECTED → join_request
     *   MEMBER_REMOVED → join_request
     *   new_set / new_comment / vocab_reminder → giữ nguyên
     */
    private String mapFeType(String rawType) {
        return switch (rawType.toUpperCase()) {
            case "JOIN_REQUEST", "JOIN_APPROVED", "JOIN_REJECTED", "MEMBER_REMOVED"
                    -> "join_request";
            default -> rawType.toLowerCase();
        };
    }

    /** Tiêu đề hiển thị theo loại thông báo */
    private String mapTitle(String rawType) {
        return switch (rawType.toUpperCase()) {
            case "JOIN_REQUEST", "JOIN_APPROVED", "JOIN_REJECTED", "MEMBER_REMOVED"
                    -> "Class Notification";
            case "NEW_SET"        -> "Flashcard Set Notification";
            case "NEW_COMMENT"    -> "Flashcard Set Notification";
            case "VOCAB_REMINDER" -> "System Notification";
            default               -> "Notification";
        };
    }

    /**
     * Build metadata cho FE dùng để điều hướng.
     *   - JOIN_REQUEST  → status=pending,  classId=idRef
     *   - JOIN_APPROVED → status=approved, classId=idRef
     *   - JOIN_REJECTED → status=rejected, classId=idRef
     *   - MEMBER_REMOVED→ status=removed,  classId=idRef
     *   - new_set / new_comment → setId=idRef
     *   - vocab_reminder → không cần metadata
     */
    private NotificationMetadata buildMetadata(String rawType, UUID idRef) {
        if (idRef == null) return null;
        String idStr = idRef.toString();

        return switch (rawType.toUpperCase()) {
            case "JOIN_REQUEST"   -> NotificationMetadata.builder()
                    .status("pending").classId(idStr).build();
            case "JOIN_APPROVED"  -> NotificationMetadata.builder()
                    .status("approved").classId(idStr).build();
            case "JOIN_REJECTED"  -> NotificationMetadata.builder()
                    .status("rejected").classId(idStr).build();
            case "MEMBER_REMOVED" -> NotificationMetadata.builder()
                    .status("removed").classId(idStr).build();
            case "NEW_SET", "new_set"         -> NotificationMetadata.builder()
                    .setId(idStr).build();
            case "NEW_COMMENT", "new_comment" -> NotificationMetadata.builder()
                    .setId(idStr).build();
            default -> null;
        };
    }
}

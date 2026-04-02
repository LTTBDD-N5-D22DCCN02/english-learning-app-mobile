package com.estudy.backend.service;

import com.estudy.backend.dto.request.ClassRequest;
import com.estudy.backend.dto.request.CopyClassRequest;
import com.estudy.backend.dto.request.JoinClassRequest;
import com.estudy.backend.dto.response.ClassMemberResponse;
import com.estudy.backend.dto.response.ClassResponse;
import com.estudy.backend.entity.Class;
import com.estudy.backend.entity.ClassMember;
import com.estudy.backend.entity.Notification;
import com.estudy.backend.entity.User;
import com.estudy.backend.enums.ClassMemberRole;
import com.estudy.backend.enums.ClassMemberStatus;
import com.estudy.backend.enums.Privacy;
import com.estudy.backend.exception.AppException;
import com.estudy.backend.exception.ErrorCode;
import com.estudy.backend.mapper.ClassMapper;
import com.estudy.backend.repository.ClassMemberRepository;
import com.estudy.backend.repository.ClassRepository;
import com.estudy.backend.repository.NotificationRepository;
import com.estudy.backend.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassService {

    ClassRepository classRepository;
    ClassMemberRepository classMemberRepository;
    NotificationRepository notificationRepository;
    UserRepository userRepository;
    ClassMapper classMapper;

    // ─── Helper: lấy user hiện tại ────────────────────────────────
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // ─── Helper: lấy class active ─────────────────────────────────
    private Class getActiveClass(UUID classId) {
        return classRepository.findByIdAndDeletedFalse(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
    }

    // ─── Helper: build ClassResponse với memberCount + myRole ──────
    private ClassResponse buildResponse(Class clazz, User currentUser) {   // đổi tham số aClass thành clazz
        ClassResponse response = classMapper.toClassResponse(clazz);

        long memberCount = classMemberRepository
                .findByClazzAndStatus(clazz, ClassMemberStatus.APPROVED)   // sửa ở đây
                .size();

        response.setMemberCount((int) memberCount);

        classMemberRepository.findByClazzAndUser(clazz, currentUser)      // sửa ở đây
                .ifPresent(cm -> response.setMyRole(cm.getRole()));

        return response;
    }

    // ─── Helper: gửi notification ─────────────────────────────────
    private void sendNotification(User recipient, String content, String type, UUID idRef) {
        Notification notification = Notification.builder()
                .user(recipient)
                .content(content)
                .type(type)
                .idRef(idRef)
                .build();
        notificationRepository.save(notification);
    }

    // ══════════════════════════════════════════════════════════════
    // UC-01: Tạo lớp học
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public ClassResponse createClass(ClassRequest request) {
        User currentUser = getCurrentUser();
        Class newClass = Class.builder()
                .name(request.getName())
                .description(request.getDescription())
                .privacy(request.getPrivacy())
                .build();
        newClass = classRepository.save(newClass);

        // Người tạo là LEADER
        ClassMember leaderMembership = ClassMember.builder()
                .clazz(newClass)
                .user(currentUser)
                .role(ClassMemberRole.LEADER)
                .status(ClassMemberStatus.APPROVED)
                .build();
        classMemberRepository.save(leaderMembership);

        return buildResponse(newClass, currentUser);
    }

    // UC-02: Sửa thông tin lớp học
    @Transactional
    public ClassResponse updateClass(UUID classId, ClassRequest request) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        ClassMember membership = classMemberRepository
                .findByClazzAndUser(aClass, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        if (membership.getRole() != ClassMemberRole.LEADER)
            throw new AppException(ErrorCode.CLASS_NOT_LEADER);

        aClass.setName(request.getName());
        aClass.setDescription(request.getDescription());
        aClass.setPrivacy(request.getPrivacy());
        classRepository.save(aClass);

        return buildResponse(aClass, currentUser);
    }

    // UC-03: Xóa lớp học
    @Transactional
    public void deleteClass(UUID classId) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        ClassMember membership = classMemberRepository
                .findByClazzAndUser(aClass, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        if (membership.getRole() != ClassMemberRole.LEADER)
            throw new AppException(ErrorCode.CLASS_NOT_LEADER);

        aClass.setDeleted(true);
        classRepository.save(aClass);
    }

    // UC-04: Xem danh sách lớp học đã tham gia
    public List<ClassResponse> getMyClasses() {
        User currentUser = getCurrentUser();
        return classMemberRepository.findApprovedByUser(currentUser).stream()
                .map(cm -> buildResponse(cm.getClazz(), currentUser))
                .collect(Collectors.toList());
    }

    // UC-05: Tham gia lớp học bằng mã lớp
    @Transactional
    public void joinClass(JoinClassRequest request) {
        User currentUser = getCurrentUser();
        Class aClass = classRepository.findByCodeAndDeletedFalse(request.getCode())
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_INVALID_CODE));

        if (classMemberRepository.existsByClazzAndUserAndStatus(aClass, currentUser, ClassMemberStatus.APPROVED))
            throw new AppException(ErrorCode.CLASS_ALREADY_MEMBER);

        if (classMemberRepository.existsByClazzAndUserAndStatus(aClass, currentUser, ClassMemberStatus.PENDING))
            throw new AppException(ErrorCode.CLASS_JOIN_REQUEST_PENDING);

        ClassMember joinRequest = ClassMember.builder()
                .clazz(aClass)
                .user(currentUser)
                .role(ClassMemberRole.MEMBER)
                .status(ClassMemberStatus.PENDING)
                .build();
        classMemberRepository.save(joinRequest);

        // Thông báo cho leader
        classMemberRepository.findLeaderByClass(aClass)
                .ifPresent(leaderMember -> sendNotification(
                        leaderMember.getUser(),
                        currentUser.getFullName() + " muốn tham gia lớp " + aClass.getName(),
                        "JOIN_REQUEST",
                        aClass.getId()
                ));
    }

    // UC-11: Tham gia lớp học công khai (Join Public Class)
    @Transactional
    public void joinPublicClass(UUID classId) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        // Kiểm tra Privacy phải là PUBLIC
        if (aClass.getPrivacy() != Privacy.PUBLIC) {
            throw new AppException(ErrorCode.CLASS_NOT_PUBLIC);
        }

        // Kiểm tra đã là thành viên APPROVED
        if (classMemberRepository.existsByClazzAndUserAndStatus(aClass, currentUser, ClassMemberStatus.APPROVED)) {
            throw new AppException(ErrorCode.CLASS_ALREADY_MEMBER);
        }

        // Kiểm tra đã có yêu cầu PENDING
        if (classMemberRepository.existsByClazzAndUserAndStatus(aClass, currentUser, ClassMemberStatus.PENDING)) {
            throw new AppException(ErrorCode.CLASS_JOIN_REQUEST_PENDING);
        }

        // Tạo yêu cầu tham gia
        ClassMember joinRequest = ClassMember.builder()
                .clazz(aClass)
                .user(currentUser)
                .role(ClassMemberRole.MEMBER)
                .status(ClassMemberStatus.PENDING)
                .build();
        classMemberRepository.save(joinRequest);

        // Gửi thông báo cho Leader
        classMemberRepository.findLeaderByClass(aClass)
                .ifPresent(leaderMember -> sendNotification(
                        leaderMember.getUser(),
                        currentUser.getFullName() + " muốn tham gia lớp " + aClass.getName(),
                        "JOIN_REQUEST",
                        aClass.getId()
                ));
    }

    // UC-12: Xem danh sách lớp học công khai (Discover)
    public List<ClassResponse> getPublicClasses(String keyword) {
        User currentUser = getCurrentUser();

        List<Class> publicClasses;

        if (keyword == null || keyword.isBlank()) {
            publicClasses = classRepository.findByPrivacyAndDeletedFalse(Privacy.PUBLIC);
        } else {
            publicClasses = classRepository.searchPublicClasses(Privacy.PUBLIC, keyword.trim());
        }

        return publicClasses.stream()
                .map(clazz -> buildResponse(clazz, currentUser))
                .collect(Collectors.toList());
    }

    // UC-06: Rời khỏi lớp học
    @Transactional
    public void leaveClass(UUID classId) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        ClassMember membership = classMemberRepository
                .findByClazzAndUser(aClass, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        if (membership.getRole() == ClassMemberRole.LEADER)
            throw new AppException(ErrorCode.CLASS_LEADER_CANNOT_LEAVE);

        classMemberRepository.delete(membership);
    }

    // UC-07 + UC-08: Xem chi tiết lớp học
    public ClassResponse getClassDetail(UUID classId) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getStatus() == ClassMemberStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        return buildResponse(aClass, currentUser);
    }

    // UC-09: Sao chép lớp học
    @Transactional
    public ClassResponse copyClass(UUID classId, CopyClassRequest request) {
        User currentUser = getCurrentUser();
        Class sourceClass = getActiveClass(classId);

        classMemberRepository.findByClazzAndUser(sourceClass, currentUser)
                .filter(cm -> cm.getStatus() == ClassMemberStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        Class newClass = Class.builder()
                .name(request.getName())
                .description(sourceClass.getDescription())
                .privacy(Privacy.PRIVATE)
                .build();
        newClass = classRepository.save(newClass);

        // Người copy làm leader của lớp mới
        ClassMember leaderMember = ClassMember.builder()
                .clazz(newClass)
                .user(currentUser)
                .role(ClassMemberRole.LEADER)
                .status(ClassMemberStatus.APPROVED)
                .build();
        classMemberRepository.save(leaderMember);

        // Gửi thông báo mời thành viên cũ (trừ người copy)
        List<ClassMember> sourceMembers = classMemberRepository
                .findByClazzAndStatus(sourceClass, ClassMemberStatus.APPROVED);

        for (ClassMember sourceMember : sourceMembers) {
            if (sourceMember.getUser().getId().equals(currentUser.getId())) continue;
            sendNotification(
                    sourceMember.getUser(),
                    currentUser.getFullName() + " đã mời bạn tham gia lớp " + newClass.getName(),
                    "CLASS_INVITE",
                    newClass.getId()
            );
        }

        return buildResponse(newClass, currentUser);
    }

    // UC-10: Tìm kiếm lớp học
    public List<ClassResponse> searchMyClasses(String keyword) {
        User currentUser = getCurrentUser();
        return classMemberRepository.findApprovedByUser(currentUser).stream()
                .map(cm -> cm.getClazz())
                .filter(c -> keyword == null || keyword.isBlank() ||
                        c.getName().toLowerCase().contains(keyword.toLowerCase()))
                .map(c -> buildResponse(c, currentUser))
                .collect(Collectors.toList());
    }

    // ─── Quản lý thành viên ───────────────────────────────────────
    public List<ClassMemberResponse> getMembers(UUID classId) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getStatus() == ClassMemberStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        return classMemberRepository.findByClazzAndStatus(aClass, ClassMemberStatus.APPROVED)
                .stream()
                .map(classMapper::toClassMemberResponse)
                .collect(Collectors.toList());
    }

    public List<ClassMemberResponse> getPendingRequests(UUID classId) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getRole() == ClassMemberRole.LEADER)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_LEADER));

        return classMemberRepository.findPendingByClass(aClass).stream()
                .map(classMapper::toClassMemberResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveRequest(UUID classId, UUID memberId) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getRole() == ClassMemberRole.LEADER)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_LEADER));

        ClassMember pending = classMemberRepository.findById(memberId)
                .filter(cm -> cm.getStatus() == ClassMemberStatus.PENDING)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_MEMBER_NOT_FOUND));

        pending.setStatus(ClassMemberStatus.APPROVED);
        classMemberRepository.save(pending);

        sendNotification(
                pending.getUser(),
                "Yêu cầu tham gia lớp " + aClass.getName() + " đã được chấp nhận",
                "JOIN_APPROVED",
                aClass.getId()
        );
    }

    @Transactional
    public void rejectRequest(UUID classId, UUID memberId) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getRole() == ClassMemberRole.LEADER)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_LEADER));

        ClassMember pending = classMemberRepository.findById(memberId)
                .filter(cm -> cm.getStatus() == ClassMemberStatus.PENDING)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_MEMBER_NOT_FOUND));

        pending.setStatus(ClassMemberStatus.REJECTED);
        classMemberRepository.save(pending);

        sendNotification(
                pending.getUser(),
                "Yêu cầu tham gia lớp " + aClass.getName() + " đã bị từ chối",
                "JOIN_REJECTED",
                aClass.getId()
        );
    }

    @Transactional
    public void removeMember(UUID classId, UUID userId) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getRole() == ClassMemberRole.LEADER)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_LEADER));

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        ClassMember membership = classMemberRepository
                .findByClazzAndUser(aClass, targetUser)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_MEMBER_NOT_FOUND));

        if (membership.getRole() == ClassMemberRole.LEADER)
            throw new AppException(ErrorCode.CLASS_NOT_LEADER);

        classMemberRepository.delete(membership);

        sendNotification(
                targetUser,
                "Bạn đã bị xóa khỏi lớp " + aClass.getName(),
                "MEMBER_REMOVED",
                aClass.getId()
        );
    }
}
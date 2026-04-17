package com.estudy.backend.service;

import com.estudy.backend.dto.request.ClassFlashCardSetRequest;
import com.estudy.backend.dto.request.ClassRequest;
import com.estudy.backend.dto.request.CopyClassRequest;
import com.estudy.backend.dto.request.JoinClassRequest;
import com.estudy.backend.dto.response.ClassMemberResponse;
import com.estudy.backend.dto.response.ClassResponse;
import com.estudy.backend.dto.response.FlashCardSetResponse;
import com.estudy.backend.entity.*;
import com.estudy.backend.entity.Class;
import com.estudy.backend.enums.ClassMemberRole;
import com.estudy.backend.enums.ClassMemberStatus;
import com.estudy.backend.enums.Privacy;
import com.estudy.backend.exception.AppException;
import com.estudy.backend.exception.ErrorCode;
import com.estudy.backend.mapper.ClassMapper;
import com.estudy.backend.mapper.FlashCardSetMapper;
import com.estudy.backend.repository.*;
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

    // THÊM vào field declarations:
    FlashCardSetRepository flashCardSetRepository;
    FlashCardSetMapper flashCardSetMapper;
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

        // Nếu đã bị REJECTED trước đó → update lại thành PENDING thay vì insert mới
        ClassMember existing = classMemberRepository
                .findByClazzAndUser(aClass, currentUser).orElse(null);

        if (existing != null) {
            existing.setStatus(ClassMemberStatus.PENDING);
            classMemberRepository.save(existing);
        } else {
            classMemberRepository.save(ClassMember.builder()
                    .clazz(aClass)
                    .user(currentUser)
                    .role(ClassMemberRole.MEMBER)
                    .status(ClassMemberStatus.PENDING)
                    .build());
        }

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
        ClassMember existing = classMemberRepository
                .findByClazzAndUser(aClass, currentUser).orElse(null);

        if (existing != null) {
            existing.setStatus(ClassMemberStatus.PENDING);
            classMemberRepository.save(existing);
        } else {
            classMemberRepository.save(ClassMember.builder()
                    .clazz(aClass)
                    .user(currentUser)
                    .role(ClassMemberRole.MEMBER)
                    .status(ClassMemberStatus.PENDING)
                    .build());
        }
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

    // UC-13: Xem danh sách thành viên
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

    // UC-14: Duyệt yêu cầu gia nhập
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

    // UC-15: Xóa thành viên khỏi lớp học
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

    // UC-16: Hiển thị mã lớp (Class Code)
    public String getClassCode(UUID classId) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        // Chỉ Leader mới xem được
        classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getRole() == ClassMemberRole.LEADER)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_LEADER));

        return aClass.getCode();
    }
    // UC-23: Tìm kiếm thành viên trong lớp
    public List<ClassMemberResponse> searchMembers(UUID classId, String keyword) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getStatus() == ClassMemberStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        String kw = (keyword == null) ? "" : keyword.toLowerCase().trim();
        return classMemberRepository.findByClazzAndStatus(aClass, ClassMemberStatus.APPROVED)
                .stream()
                .filter(cm -> kw.isBlank() ||
                        cm.getUser().getFullName().toLowerCase().contains(kw) ||
                        cm.getUser().getUsername().toLowerCase().contains(kw))
                .map(classMapper::toClassMemberResponse)
                .collect(Collectors.toList());
    }

    // UC-24: Cập nhật quyền thành viên
    @Transactional
    public void updateMemberRole(UUID classId, UUID userId, ClassMemberRole newRole) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        // Chỉ LEADER mới được thay đổi quyền
        ClassMember currentMembership = classMemberRepository
                .findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getRole() == ClassMemberRole.LEADER)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_LEADER));

        // Không được thay đổi quyền của chính mình
        if (userId.equals(currentUser.getId()))
            throw new AppException(ErrorCode.CLASS_CANNOT_CHANGE_LEADER_ROLE);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        ClassMember membership = classMemberRepository
                .findByClazzAndUser(aClass, targetUser)
                .filter(cm -> cm.getStatus() == ClassMemberStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_MEMBER_NOT_FOUND));

        // Nếu nâng lên LEADER → hạ người hiện tại xuống MEMBER (chuyển giao)
        if (newRole == ClassMemberRole.LEADER) {
            currentMembership.setRole(ClassMemberRole.MEMBER);
            classMemberRepository.save(currentMembership);
        }

        membership.setRole(newRole);
        classMemberRepository.save(membership);

        sendNotification(
                targetUser,
                "Quyền của bạn trong lớp " + aClass.getName() + " đã được cập nhật thành " + newRole.name(),
                "ROLE_UPDATED",
                aClass.getId()
        );
    }

    // UC-17: Xem danh sách bộ Flashcard trong lớp
    // UC-22: Tìm kiếm bộ Flashcard trong lớp
    public List<FlashCardSetResponse> getClassFlashCardSets(UUID classId, String keyword) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getStatus() == ClassMemberStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        List<FlashCardSet> sets;
        if (keyword == null || keyword.isBlank()) {
            sets = flashCardSetRepository.findByClazzAndDeletedFalse(aClass);
        } else {
            sets = flashCardSetRepository.searchByClassAndName(aClass, keyword.trim());
        }

        return sets.stream()
                .map(flashCardSetMapper::toFlashCardSetResponse)
                .collect(Collectors.toList());
    }

    // UC-18: Thêm bộ Flashcard vào lớp học
    @Transactional
    public FlashCardSetResponse addClassFlashCardSet(UUID classId, ClassFlashCardSetRequest request) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getStatus() == ClassMemberStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        FlashCardSet flashCardSet = FlashCardSet.builder()
                .name(request.getName())
                .description(request.getDescription())
                .privacy(request.getPrivacy())
                .user(currentUser)
                .clazz(aClass)
                .build();

        FlashCardSet saved = flashCardSetRepository.save(flashCardSet);

        // Gửi thông báo cho tất cả thành viên trong lớp (trừ người tạo)
        classMemberRepository.findByClazzAndStatus(aClass, ClassMemberStatus.APPROVED)
                .stream()
                .filter(cm -> !cm.getUser().getId().equals(currentUser.getId()))
                .forEach(cm -> sendNotification(
                        cm.getUser(),
                        currentUser.getFullName() + " đã thêm bộ flashcard \"" + saved.getName() + "\" vào lớp " + aClass.getName(),
                        "new_set",
                        saved.getId()
                ));

        return flashCardSetMapper.toFlashCardSetResponse(saved);
    }

    // UC-19: Sửa bộ Flashcard trong lớp học
    @Transactional
    public FlashCardSetResponse updateClassFlashCardSet(UUID classId, UUID setId,
                                                        ClassFlashCardSetRequest request) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        ClassMember membership = classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getStatus() == ClassMemberStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        FlashCardSet flashCardSet = flashCardSetRepository
                .findByIdAndClazzAndDeletedFalse(setId, aClass)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_FLASHCARD_SET_NOT_FOUND));

        // Chỉ Leader hoặc chủ sở hữu mới được sửa
        boolean isLeader = membership.getRole() == ClassMemberRole.LEADER;
        boolean isOwner  = flashCardSet.getUser().getId().equals(currentUser.getId());
        if (!isLeader && !isOwner)
            throw new AppException(ErrorCode.CLASS_FLASHCARD_SET_NO_PERMISSION);

        flashCardSet.setName(request.getName());
        flashCardSet.setDescription(request.getDescription());
        flashCardSet.setPrivacy(request.getPrivacy());

        return flashCardSetMapper.toFlashCardSetResponse(
                flashCardSetRepository.save(flashCardSet)
        );
    }

    // UC-20: Xóa bộ Flashcard trong lớp học
    @Transactional
    public void deleteClassFlashCardSet(UUID classId, UUID setId) {
        User currentUser = getCurrentUser();
        Class aClass = getActiveClass(classId);

        ClassMember membership = classMemberRepository.findByClazzAndUser(aClass, currentUser)
                .filter(cm -> cm.getStatus() == ClassMemberStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        FlashCardSet flashCardSet = flashCardSetRepository
                .findByIdAndClazzAndDeletedFalse(setId, aClass)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_FLASHCARD_SET_NOT_FOUND));

        boolean isLeader = membership.getRole() == ClassMemberRole.LEADER;
        boolean isOwner  = flashCardSet.getUser().getId().equals(currentUser.getId());
        if (!isLeader && !isOwner)
            throw new AppException(ErrorCode.CLASS_FLASHCARD_SET_NO_PERMISSION);

        flashCardSet.setDeleted(true);
        flashCardSetRepository.save(flashCardSet);
    }
}
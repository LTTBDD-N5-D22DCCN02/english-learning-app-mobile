package com.estudy.backend.service;

import com.estudy.backend.dto.request.FlashCardSetRequest;
import com.estudy.backend.dto.response.FlashCardSetDetailResponse;
import com.estudy.backend.dto.response.FlashCardSetResponse;
import com.estudy.backend.entity.FlashCardSet;
import com.estudy.backend.entity.User;
import com.estudy.backend.exception.AppException;
import com.estudy.backend.exception.ErrorCode;
import com.estudy.backend.mapper.FlashCardSetMapper;
import com.estudy.backend.repository.FlashCardRepository;
import com.estudy.backend.repository.FlashCardSetRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlashCardSetService {

    FlashCardRepository flashCardRepository;
    FlashCardSetRepository flashCardSetRepository;
    UserRepository userRepository;
    FlashCardSetMapper flashCardSetMapper;

    // Lấy user hiện tại từ JWT
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // 1. Tạo bộ flashcard
    @Transactional
    public FlashCardSetResponse create(FlashCardSetRequest request) {
        User currentUser = getCurrentUser();

        FlashCardSet flashCardSet = flashCardSetMapper.toFlashCardSet(request);
        flashCardSet.setUser(currentUser);

        return flashCardSetMapper.toFlashCardSetResponse(
                flashCardSetRepository.save(flashCardSet)
        );
    }

    // 2. Chỉnh sửa bộ flashcard
    @Transactional
    public FlashCardSetResponse update(UUID id, FlashCardSetRequest request) {
        User currentUser = getCurrentUser();

        FlashCardSet flashCardSet = flashCardSetRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_SET_NOT_FOUND));

        // Chỉ chủ sở hữu mới được sửa
        if (!flashCardSet.getUser().getId().equals(currentUser.getId()))
            throw new AppException(ErrorCode.FLASHCARD_SET_NOT_OWNED);

        flashCardSetMapper.updateFlashCardSet(request, flashCardSet);

        return flashCardSetMapper.toFlashCardSetResponse(
                flashCardSetRepository.save(flashCardSet)
        );
    }

    // 3. Xoá mềm bộ flashcard
    @Transactional
    public void delete(UUID id) {
        User currentUser = getCurrentUser();

        FlashCardSet flashCardSet = flashCardSetRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_SET_NOT_FOUND));

        if (!flashCardSet.getUser().getId().equals(currentUser.getId()))
            throw new AppException(ErrorCode.FLASHCARD_SET_NOT_OWNED);

        flashCardSet.setDeleted(true);
        flashCardSetRepository.save(flashCardSet);
    }

    // 4. Danh sách bộ flashcard của user đang đăng nhập
    @Transactional(readOnly = true)
    public List<FlashCardSetResponse> getMyFlashCardSets() {
        User currentUser = getCurrentUser();

        return flashCardSetRepository
                .findAllByUserIdAndDeletedFalse(currentUser.getId())
                .stream()
                .map(flashCardSetMapper::toFlashCardSetResponse)
                .toList();
    }

    // 5. Chi tiết 1 bộ flashcard
    @Transactional(readOnly = true)
    public FlashCardSetDetailResponse getDetail(UUID id) {
        User currentUser = getCurrentUser();

        FlashCardSet flashCardSet = flashCardSetRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_SET_NOT_FOUND));

        // Nếu PRIVATE thì chỉ chủ sở hữu mới xem được
        if (flashCardSet.getPrivacy().name().equals("PRIVATE")
                && !flashCardSet.getUser().getId().equals(currentUser.getId()))
            throw new AppException(ErrorCode.FLASHCARD_SET_NOT_OWNED);

        // Map thủ công để đảm bảo chỉ lấy flashcard chưa bị xóa
        FlashCardSetDetailResponse response = flashCardSetMapper.toFlashCardSetDetailResponse(flashCardSet);

        // Override flashCards với list đã filter deleted = false
        List<com.estudy.backend.entity.FlashCard> activeCards =
                flashCardRepository.findByFlashCardSetIdAndDeletedFalse(flashCardSet.getId());
        response.setFlashCards(
                activeCards.stream()
                        .map(flashCardSetMapper::toFlashCardResponse)
                        .collect(java.util.stream.Collectors.toList())
        );

        return response;
    }
}
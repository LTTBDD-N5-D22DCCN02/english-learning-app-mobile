package com.estudy.backend.service;

import com.estudy.backend.dto.request.CommentRequest;
import com.estudy.backend.dto.response.CommentResponse;
import com.estudy.backend.entity.Comment;
import com.estudy.backend.entity.FlashCardSet;
import com.estudy.backend.entity.User;
import com.estudy.backend.exception.AppException;
import com.estudy.backend.exception.ErrorCode;
import com.estudy.backend.mapper.CommentMapper;
import com.estudy.backend.repository.CommentRepository;
import com.estudy.backend.repository.FlashCardSetRepository;
import com.estudy.backend.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentService {

    CommentRepository commentRepository;
    FlashCardSetRepository flashCardSetRepository;
    UserRepository userRepository;
    CommentMapper commentMapper;

    // Lấy user hiện tại từ JWT
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // 1. Thêm comment vào 1 bộ flashcard
    @Transactional
    public CommentResponse create(UUID flashCardSetId, CommentRequest request) {
        User currentUser = getCurrentUser();

        FlashCardSet flashCardSet = flashCardSetRepository.findByIdAndDeletedFalse(flashCardSetId)
                .orElseThrow(() -> new AppException(ErrorCode.FLASHCARD_SET_NOT_FOUND));

        Comment comment = commentMapper.toComment(request);
        comment.setUser(currentUser);
        comment.setFlashCardSet(flashCardSet);

        return commentMapper.toCommentResponse(commentRepository.save(comment));
    }

    // 2. Xoá comment (chủ comment hoặc chủ bộ flashcard)
    @Transactional
    public void delete(UUID commentId) {
        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        boolean isCommentOwner = comment.getUser().getId().equals(currentUser.getId());
        boolean isFlashCardSetOwner = comment.getFlashCardSet().getUser().getId().equals(currentUser.getId());

        if (!isCommentOwner && !isFlashCardSetOwner)
            throw new AppException(ErrorCode.COMMENT_NOT_OWNED);

        comment.setDeleted(true);
        commentRepository.save(comment);
    }
}
package com.shu.backend.domain.board.service;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.exception.BoardException;
import com.shu.backend.domain.board.exception.status.BoardErrorStatus;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.enums.CommentStatus;
import com.shu.backend.domain.comment.exception.CommentException;
import com.shu.backend.domain.comment.exception.status.CommentErrorStatus;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.enums.PostStatus;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardAccessPolicy {

    private final UserRepository userRepository;

    public User requireActiveUserWithSchool(Long userId) {
        User user = userRepository.findByIdWithSchoolAndRegion(userId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        if (user.getStatus() != null && user.getStatus() != UserStatus.ACTIVE) {
            throw new UserException(UserErrorStatus.INACTIVE_USER);
        }
        if (user.getSchool() == null) {
            throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_REQUIRED);
        }
        return user;
    }

    public void assertSchoolMember(Long userId, Long schoolId) {
        User user = requireActiveUserWithSchool(userId);
        if (schoolId == null || !schoolId.equals(user.getSchool().getId())) {
            throw new PostException(PostErrorStatus.NO_PERMISSION_TO_ACCESS);
        }
    }

    public void assertCanAccessBoard(Long userId, Board board) {
        if (board == null) {
            throw new BoardException(BoardErrorStatus.BOARD_NOT_FOUND);
        }
        if (!board.isActive()) {
            throw new BoardException(BoardErrorStatus.BOARD_INACTIVE);
        }

        User user = requireActiveUserWithSchool(userId);
        if (board.getScope() == BoardScope.SCHOOL) {
            if (board.getSchool() == null || !board.getSchool().getId().equals(user.getSchool().getId())) {
                throw new PostException(PostErrorStatus.NO_PERMISSION_TO_ACCESS);
            }
            return;
        }

        if (board.getScope() == BoardScope.REGION) {
            Long userRegionId = user.getSchool().getRegion() != null
                    ? user.getSchool().getRegion().getId()
                    : null;
            if (board.getRegion() == null || userRegionId == null || !board.getRegion().getId().equals(userRegionId)) {
                throw new PostException(PostErrorStatus.NO_PERMISSION_TO_ACCESS);
            }
            return;
        }

        throw new BoardException(BoardErrorStatus.INVALID_BOARD_SCOPE);
    }

    public void assertCanAccessPost(Long userId, Post post) {
        if (post == null) {
            throw new PostException(PostErrorStatus.POST_NOT_FOUND);
        }
        if (post.getPostStatus() == PostStatus.DELETED) {
            throw new PostException(PostErrorStatus.POST_ALREADY_DELETED);
        }
        assertCanAccessBoard(userId, post.getBoard());
    }

    public void assertCanAccessComment(Long userId, Comment comment) {
        if (comment == null) {
            throw new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND);
        }
        if (comment.getCommentStatus() == CommentStatus.DELETED) {
            throw new CommentException(CommentErrorStatus.COMMENT_ALREADY_DELETED);
        }
        assertCanAccessPost(userId, comment.getPost());
    }
}

package com.shu.backend.domain.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.enums.CommentStatus;
import com.shu.backend.domain.user.support.UserDisplay;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Builder
public class CommentResponse {

    private Long commentId;
    @JsonProperty("isMine")
    private boolean isMine;
    @JsonProperty("isPostAuthor")
    private boolean isPostAuthor;
    private Long authorUserId;
    private boolean authorDeleted;
    private boolean canChatWithAuthor;
    private boolean canReportAuthor;
    private boolean canBlockAuthor;
    private String commentStatus;
    private String content;
    private String author;
    private int likeCount;
    private int dislikeCount;
    @JsonProperty("likedByMe")
    private boolean likedByMe;
    private boolean anonymous;
    private int depth;
    private Long parentId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("createdAtMs")
    public Long getCreatedAtMs() {
        return createdAt != null
                ? createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : null;
    }

    public static CommentResponse toDto(Comment comment, Long currentUserId, Long postAuthorId) {
        return toDto(comment, currentUserId, postAuthorId, false, 0);
    }

    public static CommentResponse toDto(Comment comment, Long currentUserId, Long postAuthorId, boolean likedByMe, int anonymousNumber) {
        String content = switch (comment.getCommentStatus()) {
            case DELETED -> "삭제된 댓글입니다.";
            case HIDDEN -> "블라인드 처리된 댓글입니다.";
            default -> comment.getContent();
        };

        boolean authorDeleted = UserDisplay.isDeleted(comment.getUser());
        String author;
        if (authorDeleted) {
            author = UserDisplay.DELETED_USER_NAME;
        } else if (comment.getAnonymous()) {
            author = anonymousNumber > 0 ? "익명" + anonymousNumber : "익명";
        } else {
            author = comment.getUser().getNickname();
        }

        boolean isMine = !authorDeleted && comment.getUser().getId().equals(currentUserId);
        boolean isPostAuthor = !authorDeleted
                && postAuthorId != null
                && comment.getUser().getId().equals(postAuthorId);
        boolean canActOnAuthor = !authorDeleted
                && !isMine
                && comment.getCommentStatus() == CommentStatus.ACTIVE;
        Long authorUserId = canActOnAuthor || isMine ? comment.getUser().getId() : null;

        return CommentResponse.builder()
                .commentId(comment.getId())
                .isMine(isMine)
                .isPostAuthor(isPostAuthor)
                .authorUserId(authorUserId)
                .authorDeleted(authorDeleted)
                .canChatWithAuthor(canActOnAuthor)
                .canReportAuthor(canActOnAuthor)
                .canBlockAuthor(canActOnAuthor)
                .commentStatus(comment.getCommentStatus().name())
                .content(content)
                .author(author)
                .likeCount(comment.getLikeCount())
                .dislikeCount(comment.getDislikeCount())
                .likedByMe(likedByMe)
                .anonymous(comment.getAnonymous())
                .depth(comment.getDepth())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}

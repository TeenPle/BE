package com.shu.backend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shu.backend.domain.comment.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCommentResponse {
    private Long commentId;
    private Long authorUserId;
    private String authorLabel;
    private boolean anonymous;
    private String commentStatus;
    private String content;
    private int likeCount;
    private int dislikeCount;
    private int depth;
    private Long parentId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static AdminCommentResponse from(Comment comment) {
        Long authorId = comment.getUser() != null ? comment.getUser().getId() : null;
        boolean anonymous = Boolean.TRUE.equals(comment.getAnonymous());
        String label = anonymous
                ? "익명 사용자 #" + authorId
                : comment.getUser() != null ? comment.getUser().getNickname() : "알 수 없음";

        return AdminCommentResponse.builder()
                .commentId(comment.getId())
                .authorUserId(authorId)
                .authorLabel(label)
                .anonymous(anonymous)
                .commentStatus(comment.getCommentStatus().name())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .dislikeCount(comment.getDislikeCount())
                .depth(comment.getDepth())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}

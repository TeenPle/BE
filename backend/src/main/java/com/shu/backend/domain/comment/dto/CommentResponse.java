package com.shu.backend.domain.comment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.enums.CommentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

    private Long commentId;
    private Long authorUserId;
    @JsonProperty("isMine")
    private boolean isMine;
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

    public static CommentResponse toDto(Comment comment, Long currentUserId, boolean likedByMe, int anonymousNumber) {
        String content;
        String author;

        switch (comment.getCommentStatus()) {
            case DELETED -> content = "삭제된 댓글입니다.";
            case HIDDEN -> content = "블라인드 처리된 댓글입니다.";
            default -> content = comment.getContent();
        }

        if (comment.getAnonymous()) {
            author = anonymousNumber > 0 ? "익명" + anonymousNumber : "익명";
        } else if (comment.getUser() != null) {
            author = comment.getUser().getNickname();
        } else {
            author = "알 수 없음";
        }

        boolean isMine = comment.getUser() != null && comment.getUser().getId().equals(currentUserId);

        return CommentResponse.builder()
                .commentId(comment.getId())
                .authorUserId(comment.getUser() != null ? comment.getUser().getId() : null)
                .isMine(isMine)
                .commentStatus(comment.getCommentStatus().name())
                .content(content)
                .author(author)
                .likeCount(comment.getLikeCount())
                .dislikeCount(comment.getDislikeCount())
                .likedByMe(likedByMe)
                .anonymous(comment.getAnonymous())
                .depth(comment.getDepth())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .build();
    }
}
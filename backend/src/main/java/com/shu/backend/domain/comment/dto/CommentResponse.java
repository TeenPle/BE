package com.shu.backend.domain.comment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.enums.CommentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

    private Long commentId;       // 댓글 ID
    @JsonProperty("isMine")
    private boolean isMine;       // 내 댓글 여부
    private Long authorUserId;    // 작성자 userId (채팅 유입용, 삭제된 경우 null)
    private String commentStatus; // 댓글 상태 (ACTIVE, DELETED, HIDDEN)
    private String content;       // 댓글 내용
    private String author;        // 작성자 이름
    private int likeCount;        // 좋아요 수
    private int dislikeCount;     // 싫어요 수
    private boolean anonymous;    // 익명 여부
    private int depth;            // 대댓글 깊이
    private Long parentId;        // 부모 댓글 ID (대댓글일 경우)

    // Comment 엔티티를 CommentResponse로 변환
    public static CommentResponse toDto(Comment comment, Long currentUserId) {

        String content;
        String author;

        //댓글 상태 처리
        switch (comment.getCommentStatus()) {
            case DELETED -> content = "삭제된 댓글입니다.";
            case HIDDEN -> content = "블라인드 처리된 댓글입니다.";
            default -> content = comment.getContent();
        }

        //익명 여부에 따른 author 반환
        if (comment.getAnonymous()) {
            author = "익명";
        } else if (comment.getUser() != null) {
            author = comment.getUser().getNickname();
        } else {
            author = "알 수 없음";
        }

        boolean isMine = comment.getUser() != null && comment.getUser().getId().equals(currentUserId);

        // 삭제/숨김 댓글은 authorUserId를 노출하지 않음
        Long authorUserId = (comment.getCommentStatus() == com.shu.backend.domain.comment.enums.CommentStatus.ACTIVE
                && comment.getUser() != null)
                ? comment.getUser().getId()
                : null;

        return CommentResponse.builder()
                .commentId(comment.getId())
                .isMine(isMine)
                .authorUserId(authorUserId)
                .commentStatus(comment.getCommentStatus().name())
                .content(content)
                .author(author)
                .likeCount(comment.getLikeCount())
                .dislikeCount(comment.getDislikeCount())
                .anonymous(comment.getAnonymous())
                .depth(comment.getDepth())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .build();
    }
}
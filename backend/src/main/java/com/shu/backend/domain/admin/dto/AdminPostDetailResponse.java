package com.shu.backend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shu.backend.domain.post.dto.PostMediaResponse;
import com.shu.backend.domain.post.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminPostDetailResponse {
    private Long postId;
    private String title;
    private String content;
    private String postStatus;
    private boolean anonymous;
    private Long authorUserId;
    private String authorLabel;
    private Long boardId;
    private String boardTitle;
    private Long schoolId;
    private String schoolName;
    private Long regionId;
    private String regionName;
    private int viewCount;
    private int likeCount;
    private int dislikeCount;
    private int commentCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private List<PostMediaResponse> mediaList;
    private List<AdminCommentResponse> comments;

    public static AdminPostDetailResponse from(
            Post post,
            List<PostMediaResponse> mediaList,
            List<AdminCommentResponse> comments
    ) {
        Long authorId = post.getUser() != null ? post.getUser().getId() : null;
        boolean anonymous = Boolean.TRUE.equals(post.getAnonymous());

        return AdminPostDetailResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .postStatus(post.getPostStatus().name())
                .anonymous(anonymous)
                .authorUserId(authorId)
                .authorLabel(anonymous ? "익명 사용자 #" + authorId : post.getUser().getNickname())
                .boardId(post.getBoard().getId())
                .boardTitle(post.getBoard().getTitle())
                .schoolId(post.getBoard().getSchool() != null ? post.getBoard().getSchool().getId() : null)
                .schoolName(post.getBoard().getSchool() != null ? post.getBoard().getSchool().getName() : null)
                .regionId(post.getBoard().getRegion() != null ? post.getBoard().getRegion().getId() : null)
                .regionName(post.getBoard().getRegion() != null ? post.getBoard().getRegion().getName() : null)
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .mediaList(mediaList)
                .comments(comments)
                .build();
    }
}

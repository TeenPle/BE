package com.shu.backend.domain.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shu.backend.domain.comment.dto.CommentResponse;
import com.shu.backend.domain.post.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PostDetailResponse {

    private Long postId;
    private Long authorUserId;
    @JsonProperty("isMine")
    private boolean isMine;
    private String title;
    private String content;
    private Integer viewCount;
    private Boolean anonymous;
    private int likeCount;
    private int dislikeCount;
    private String postStatus;
    private String username;
    private String authorProfileImageUrl;
    private List<CommentResponse> comments;
    private List<PostMediaResponse> mediaList;
    @JsonProperty("isBookmarked")
    private boolean isBookmarked;


    public static PostDetailResponse toDto(Post post, List<CommentResponse> comments, List<PostMediaResponse> mediaList, Long currentUserId, boolean isBookmarked) {
        String profileImageUrl = post.getAnonymous() ? null : post.getUser().getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.startsWith("http")) {
            profileImageUrl = null;
        }
        return PostDetailResponse.builder()
                .postId(post.getId())
                .authorUserId(post.getUser().getId())
                .isMine(post.getUser().getId().equals(currentUserId))
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .anonymous(post.getAnonymous())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .postStatus(post.getPostStatus().name())
                .username(post.getUser().getNickname())
                .authorProfileImageUrl(profileImageUrl)
                .comments(comments)
                .mediaList(mediaList)
                .isBookmarked(isBookmarked)
                .build();
    }
}

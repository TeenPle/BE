package com.shu.backend.domain.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shu.backend.domain.post.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Getter
@Builder
public class PostResponse {

    private Long id;
    private String title;
    private String content;
    private String postStatus; // 게시글 상태
    private Integer viewCount; // 조회수
    private Boolean anonymous; // 익명 여부
    private int likeCount; // 좋아요 수
    private int dislikeCount; // 싫어요 수
    private Long boardId;  // 게시판 ID
    private Long userId;   // 사용자 ID
    private String username; // 사용자 이름
    private String authorProfileImageUrl; // 작성자 프로필 이미지 URL (익명이면 null)
    private int commentCount; // 댓글 수 (선택적)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @Builder.Default
    private List<PostMediaResponse> mediaList = List.of();
    private boolean hasPoll;

    @JsonProperty("createdAtMs")
    public Long getCreatedAtMs() {
        return createdAt != null
                ? createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : null;
    }

    // 미디어를 덧붙인 복사본 반환
    public PostResponse withMedia(List<PostMediaResponse> mediaList) {
        return PostResponse.builder()
                .id(this.id)
                .title(this.title)
                .content(this.content)
                .postStatus(this.postStatus)
                .viewCount(this.viewCount)
                .anonymous(this.anonymous)
                .likeCount(this.likeCount)
                .dislikeCount(this.dislikeCount)
                .boardId(this.boardId)
                .userId(this.userId)
                .username(this.username)
                .authorProfileImageUrl(this.authorProfileImageUrl)
                .commentCount(this.commentCount)
                .createdAt(this.createdAt)
                .mediaList(mediaList)
                .hasPoll(this.hasPoll)
                .build();
    }

    // Post 엔티티를 PostResponse로 변환하는 메서드
    public static PostResponse toDto(Post post, int commentCount) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .postStatus(post.getPostStatus().name()) // Enum을 문자열로 변환
                .viewCount(post.getViewCount())
                .anonymous(post.getAnonymous())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .boardId(post.getBoard().getId()) // 게시판 ID
                .userId(post.getUser().getId()) // 사용자 ID
                .username(post.getUser().getUsername()) // 사용자 이름
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .hasPoll(false)
                .build();
    }

    // 추가: 쿼리 row(Object[]) → PostResponse
    public static PostResponse fromRow(Object[] r) {
        // r 인덱스는 Repository 쿼리 select 순서와 반드시 동일해야 합니다.
        Long id = (Long) r[0];
        String title = (String) r[1];
        String content = (String) r[2];

        // enum이면 enum으로 들어옵니다 (JPQL에서 p.postStatus 선택 시)
        String postStatus = (r[3] instanceof Enum<?> e) ? e.name() : String.valueOf(r[3]);

        Integer viewCount = (Integer) r[4];
        Boolean anonymous = (Boolean) r[5];
        Integer likeCount = (Integer) r[6];
        Integer dislikeCount = (Integer) r[7];
        Long boardId = (Long) r[8];
        Long userId = (Long) r[9];
        String username = (String) r[10];

        // JPA count는 Long으로 오는 경우가 일반적
        int commentCount = (r[11] == null) ? 0 : ((Number) r[11]).intValue();

        // 익명 게시글이면 프로필 이미지 null, 아니면 URL 검증 후 반환
        String rawProfileUrl = (r.length > 12) ? (String) r[12] : null;
        String authorProfileImageUrl = null;
        if (Boolean.FALSE.equals(anonymous) && rawProfileUrl != null && rawProfileUrl.startsWith("http")) {
            authorProfileImageUrl = rawProfileUrl;
        }

        LocalDateTime createdAt = (r.length > 13) ? (LocalDateTime) r[13] : null;
        Boolean hasPoll = (r.length > 14) ? (Boolean) r[14] : false;

        return PostResponse.builder()
                .id(id)
                .title(title)
                .content(content)
                .postStatus(postStatus)
                .viewCount(viewCount)
                .anonymous(anonymous)
                .likeCount(likeCount == null ? 0 : likeCount)
                .dislikeCount(dislikeCount == null ? 0 : dislikeCount)
                .boardId(boardId)
                .userId(userId)
                .username(username)
                .authorProfileImageUrl(authorProfileImageUrl)
                .commentCount(commentCount)
                .createdAt(createdAt)
                .hasPoll(Boolean.TRUE.equals(hasPoll))
                .build();
    }
}

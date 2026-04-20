package com.shu.backend.domain.post.dto;

import com.shu.backend.domain.media.entity.Media;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostMediaResponse {

    private Long mediaId;
    private String url;
    private String mediaType;

    public static PostMediaResponse from(Media media) {
        return PostMediaResponse.builder()
                .mediaId(media.getId())
                .url(media.getUrl())
                .mediaType(media.getMediaType().name())
                .build();
    }
}

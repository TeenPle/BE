package com.shu.backend.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CommentCreateRequest {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 500, message = "댓글은 500자를 초과할 수 없습니다.")
    private String content;

    private Boolean anonymous = true;

    private Long parentId = null;
}
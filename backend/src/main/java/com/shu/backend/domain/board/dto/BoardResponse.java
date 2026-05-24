package com.shu.backend.domain.board.dto;

import com.shu.backend.domain.board.entity.Board;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardResponse {

    private Long id;

    private String title;

    private String description;

    private boolean active;

    // 게시판 범위 (SCHOOL: 학교 게시판, REGION: 지역 게시판)
    private String scope;

    private String type;

    private boolean defaultBoard;

    private int sortOrder;

    public static BoardResponse toDto(Board board) {
        return BoardResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .description(board.getDescription())
                .active(board.isActive())
                // scope를 문자열로 직렬화 (null-safe)
                .scope(board.getScope() != null ? board.getScope().name() : null)
                .type(board.getType() != null ? board.getType().name() : null)
                .defaultBoard(board.isDefaultBoard())
                .sortOrder(board.getSortOrder())
                .build();
    }
}

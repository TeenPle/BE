package com.shu.backend.domain.admin.dto;

import com.shu.backend.domain.board.entity.Board;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminBoardResponse {
    private Long id;
    private String title;
    private String description;
    private String scope;
    private boolean active;
    private Long schoolId;
    private String schoolName;
    private Long regionId;
    private String regionName;
    private int postCount;

    public static AdminBoardResponse from(Board board, int postCount) {
        return AdminBoardResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .description(board.getDescription())
                .scope(board.getScope() != null ? board.getScope().name() : null)
                .active(board.isActive())
                .schoolId(board.getSchool() != null ? board.getSchool().getId() : null)
                .schoolName(board.getSchool() != null ? board.getSchool().getName() : null)
                .regionId(board.getRegion() != null ? board.getRegion().getId() : null)
                .regionName(board.getRegion() != null ? board.getRegion().getName() : null)
                .postCount(postCount)
                .build();
    }
}

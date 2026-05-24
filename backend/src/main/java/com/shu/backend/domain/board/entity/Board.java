package com.shu.backend.domain.board.entity;

import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.enums.BoardType;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "board",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_board_school_title", columnNames = {"school_id", "title"}),
                @UniqueConstraint(name = "uq_board_region_title", columnNames = {"region_id", "title"})
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private java.lang.Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BoardScope scope = BoardScope.SCHOOL;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BoardType type;

    @Builder.Default
    @Column(name = "default_board", nullable = false)
    private boolean defaultBoard = false;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 999;

    public void markAsDefault(BoardType type) {
        this.type = type;
        this.defaultBoard = true;
        this.sortOrder = type.getSortOrder();
        this.active = true;
        this.scope = BoardScope.SCHOOL;
        this.description = type.getDescription();
    }
}

package com.shu.backend.domain.boardprofile.entity;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "board_display_profile",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_board_display_profile_user_board", columnNames = {"user_id", "board_id"}),
                @UniqueConstraint(name = "uq_board_display_profile_board_name", columnNames = {"board_id", "display_name"})
        },
        indexes = {
                @Index(name = "idx_board_display_profile_user", columnList = "user_id"),
                @Index(name = "idx_board_display_profile_board", columnList = "board_id"),
                @Index(name = "idx_board_display_profile_user_board", columnList = "user_id, board_id"),
                @Index(name = "idx_board_display_profile_board_name", columnList = "board_id, display_name")
        }
)
public class BoardDisplayProfile extends BaseEntity {

    public static final String DEFAULT_PROFILE_IMAGE_URL = "default_profile.png";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false, length = 20)
    private String adjective;

    @Column(nullable = false, length = 4)
    private String number;

    @Column(name = "display_name", nullable = false, length = 40)
    private String displayName;

    @Column(name = "profile_image_url")
    @Builder.Default
    private String profileImageUrl = DEFAULT_PROFILE_IMAGE_URL;

    @Column(name = "last_changed_at")
    private LocalDateTime lastChangedAt;

    @Column(name = "next_change_available_at")
    private LocalDateTime nextChangeAvailableAt;

    public void update(String adjective, String number, String displayName, String profileImageUrl, LocalDateTime changedAt, LocalDateTime nextChangeAvailableAt) {
        this.adjective = adjective;
        this.number = number;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
        this.lastChangedAt = changedAt;
        this.nextChangeAvailableAt = nextChangeAvailableAt;
    }
}

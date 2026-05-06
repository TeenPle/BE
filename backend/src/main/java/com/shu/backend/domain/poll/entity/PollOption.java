package com.shu.backend.domain.poll.entity;

import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Table(
        name = "poll_option",
        indexes = {
                @Index(name = "idx_poll_option_poll_id", columnList = "poll_id")
        }
)
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PollOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(nullable = false, length = 100)
    private String text;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "vote_count", nullable = false)
    @Builder.Default
    private int voteCount = 0;

    public void incrementVoteCount() {
        this.voteCount++;
    }
}

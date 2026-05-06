package com.shu.backend.domain.poll.entity;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Table(
        name = "poll_vote",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_poll_vote_poll_user", columnNames = {"poll_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_poll_vote_poll_id", columnList = "poll_id"),
                @Index(name = "idx_poll_vote_option_id", columnList = "poll_option_id"),
                @Index(name = "idx_poll_vote_user_id", columnList = "user_id")
        }
)
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PollVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_option_id", nullable = false)
    private PollOption option;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

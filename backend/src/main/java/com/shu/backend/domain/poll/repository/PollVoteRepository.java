package com.shu.backend.domain.poll.repository;

import com.shu.backend.domain.poll.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    boolean existsByPollIdAndUserId(Long pollId, Long userId);

    Optional<PollVote> findByPollIdAndUserId(Long pollId, Long userId);

    long countByPollId(Long pollId);

    void deleteByPollId(Long pollId);
}

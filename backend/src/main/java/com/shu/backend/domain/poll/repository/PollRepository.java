package com.shu.backend.domain.poll.repository;

import com.shu.backend.domain.poll.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {
    boolean existsByPostId(Long postId);

    Optional<Poll> findByPostId(Long postId);

    List<Poll> findByPostIdIn(Collection<Long> postIds);

    void deleteByPostId(Long postId);
}

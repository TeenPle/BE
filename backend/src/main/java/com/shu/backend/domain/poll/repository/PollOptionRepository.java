package com.shu.backend.domain.poll.repository;

import com.shu.backend.domain.poll.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    List<PollOption> findByPollIdOrderByDisplayOrderAsc(Long pollId);

    void deleteByPollId(Long pollId);
}

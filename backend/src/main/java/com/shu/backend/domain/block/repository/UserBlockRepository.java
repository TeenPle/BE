package com.shu.backend.domain.block.repository;

import com.shu.backend.domain.block.entity.UserBlock;
import com.shu.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("select ub.blocked from UserBlock ub where ub.blocker.id = :blockerId order by ub.createdAt desc")
    List<User> findBlockedUsers(@Param("blockerId") Long blockerId);
}

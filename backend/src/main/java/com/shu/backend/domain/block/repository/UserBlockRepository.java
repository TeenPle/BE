package com.shu.backend.domain.block.repository;

import com.shu.backend.domain.block.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    long countByBlockerId(Long blockerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserBlock b where b.blocker.id = :blockerId")
    void deleteByBlockerId(@Param("blockerId") Long blockerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserBlock b where b.blocked.id = :blockedId")
    void deleteByBlockedId(@Param("blockedId") Long blockedId);
}

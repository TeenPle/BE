package com.shu.backend.domain.reaction.repository;

import com.shu.backend.domain.reaction.entity.Reaction;
import com.shu.backend.domain.reaction.enums.ReactionTargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByUserIdAndTargetTypeAndTargetId(Long userId, ReactionTargetType targetType, Long targetId);

    @Query("""
        select r.targetId
        from Reaction r
        where r.userId = :userId
          and r.targetType = com.shu.backend.domain.reaction.enums.ReactionTargetType.POST
          and r.liked = true
        order by r.id desc
    """)
    List<Long> findLikedPostIds(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        select r.targetId
        from Reaction r
        where r.userId = :userId
          and r.targetType = com.shu.backend.domain.reaction.enums.ReactionTargetType.COMMENT
          and r.targetId in :commentIds
          and r.liked = true
    """)
    Set<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);
}

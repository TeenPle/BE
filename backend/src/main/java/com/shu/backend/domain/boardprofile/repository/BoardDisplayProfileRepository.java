package com.shu.backend.domain.boardprofile.repository;

import com.shu.backend.domain.boardprofile.entity.BoardDisplayProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BoardDisplayProfileRepository extends JpaRepository<BoardDisplayProfile, Long> {

    Optional<BoardDisplayProfile> findByUserIdAndBoardId(Long userId, Long boardId);

    boolean existsByBoardIdAndDisplayName(Long boardId, String displayName);

    @Query("""
        select p
        from BoardDisplayProfile p
        join fetch p.board b
        where p.user.id = :userId
        order by b.sortOrder asc, b.id asc
    """)
    List<BoardDisplayProfile> findAllByUserIdWithBoard(@Param("userId") Long userId);

    @Query("""
        select p
        from BoardDisplayProfile p
        where p.board.id = :boardId
          and p.user.id in :userIds
    """)
    List<BoardDisplayProfile> findByBoardIdAndUserIdIn(
            @Param("boardId") Long boardId,
            @Param("userIds") Collection<Long> userIds
    );
}

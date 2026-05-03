package com.shu.backend.domain.bookmark.repository;

import com.shu.backend.domain.bookmark.entity.Bookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndPostId(Long userId, Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    @Query("""
        select b from Bookmark b
        join fetch b.post p
        join fetch p.user u
        join fetch p.board bo
        where b.user.id = :userId
          and p.postStatus = 'ACTIVE'
        order by b.createdAt desc
    """)
    List<Bookmark> findByUserIdOrderByCreatedAtDesc(
            @Param("userId") Long userId, Pageable pageable);
}

package com.shu.backend.domain.post.repository;

import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.enums.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Slice<Post> findByBoardId(Long boardId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount + :delta where p.id = :postId")
    int updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.dislikeCount = p.dislikeCount + :delta where p.id = :postId")
    int updateDislikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Query("""
        select p.id
        from Post p
        where p.board.id = :boardId
          and p.postStatus = :status
        order by p.id desc
    """)
    List<Long> findPostIdsByBoardId(
            @Param("boardId") Long boardId,
            @Param("status") PostStatus status,
            Pageable pageable
    );

    @Query("""
        select
            p.id,
            p.title,
            p.content,
            p.postStatus,
            p.viewCount,
            p.anonymous,
            p.likeCount,
            p.dislikeCount,
            b.id,
            u.id,
            u.username,
            (
                select count(c.id)
                from Comment c
                where c.post.id = p.id
                   and c.commentStatus <> com.shu.backend.domain.comment.enums.CommentStatus.DELETED
            )
        from Post p
        join p.board b
        join p.user u
        where b.id = :boardId
        order by p.id desc
    """)
    List<Object[]> findPostRowsByBoardId(Long boardId, Pageable pageable);
}

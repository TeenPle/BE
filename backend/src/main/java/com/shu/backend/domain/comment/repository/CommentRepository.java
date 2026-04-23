package com.shu.backend.domain.comment.repository;

import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.enums.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostId(Long postId);

    int countByPostId(Long postId);

    @Query("""
        select c
        from Comment c
        join fetch c.user u
        where c.post.id = :postId
          and c.parent is null
          and c.commentStatus in :statuses
        order by c.createdAt asc
    """)
    List<Comment> findParentsForPostDetail(
            @Param("postId") Long postId,
            @Param("statuses") List<CommentStatus> statuses
    );

    default List<Comment> findParentsForPostDetail(Long postId) {
        // DELETED 포함 — 자식이 있으면 "삭제된 댓글입니다." 플레이스홀더로 표시해야 하므로
        return findParentsForPostDetail(postId, List.of(CommentStatus.ACTIVE, CommentStatus.HIDDEN, CommentStatus.DELETED));
    }

    @Query("""
    select c
    from Comment c
    join fetch c.user u
    where c.parent.id = :parentId
      and c.commentStatus in :statuses
    order by c.createdAt asc
""")
    List<Comment> findReplies(Long parentId, List<CommentStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.likeCount = c.likeCount + :delta where c.id = :commentId")
    int updateLikeCount(@Param("commentId") Long commentId, @Param("delta") int delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.dislikeCount = c.dislikeCount + :delta where c.id = :commentId")
    int updateDislikeCount(@Param("commentId") Long commentId, @Param("delta") int delta);

    @Query("""
    select c from Comment c
    join fetch c.user u
    where c.parent.id in :parentIds
    order by c.parent.id asc, c.createdAt asc
""")
    List<Comment> findChildrenByParentIds(List<Long> parentIds);

    @Query("""
        select
            c.id,
            c.content,
            c.post.id,
            c.post.title,
            c.likeCount,
            c.createdAt
        from Comment c
        where c.user.id = :userId
          and c.commentStatus <> com.shu.backend.domain.comment.enums.CommentStatus.DELETED
        order by c.id desc
    """)
    List<Object[]> findMyCommentRows(@Param("userId") Long userId, Pageable pageable);
}

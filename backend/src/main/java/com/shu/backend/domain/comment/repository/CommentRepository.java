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

    @Query("""
        select c
        from Comment c
        join fetch c.user u
        left join fetch c.parent p
        where c.post.id = :postId
        order by c.createdAt asc
    """)
    List<Comment> findAdminCommentsByPostId(@Param("postId") Long postId);

    int countByPostId(Long postId);

    @Query("select count(c) from Comment c where c.user.id = :userId and c.commentStatus <> com.shu.backend.domain.comment.enums.CommentStatus.DELETED")
    long countActiveByUserId(@Param("userId") Long userId);

    @Query("""
        select c
        from Comment c
        join fetch c.user u
        where c.post.id = :postId
          and c.parent is null
          and c.commentStatus in :statuses
          and c.user.id not in (
              select ub.blocked.id from com.shu.backend.domain.block.entity.UserBlock ub
              where ub.blocker.id = :currentUserId
          )
        order by c.createdAt asc
    """)
    List<Comment> findParentsForPostDetail(
            @Param("postId") Long postId,
            @Param("statuses") List<CommentStatus> statuses,
            @Param("currentUserId") Long currentUserId
    );

    default List<Comment> findParentsForPostDetail(Long postId, Long currentUserId) {
        return findParentsForPostDetail(postId, List.of(CommentStatus.ACTIVE, CommentStatus.HIDDEN, CommentStatus.DELETED), currentUserId);
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
      and c.user.id not in (
          select ub.blocked.id from com.shu.backend.domain.block.entity.UserBlock ub
          where ub.blocker.id = :currentUserId
      )
    order by c.parent.id asc, c.createdAt asc
""")
    List<Comment> findChildrenByParentIds(@Param("parentIds") List<Long> parentIds, @Param("currentUserId") Long currentUserId);

    @Query("""
        select
            c.id,
            c.content,
            c.post.id,
            c.post.title,
            c.likeCount,
            c.createdAt,
            c.post.board.title
        from Comment c
        where c.user.id = :userId
          and c.commentStatus <> com.shu.backend.domain.comment.enums.CommentStatus.DELETED
        order by c.id desc
    """)
    List<Object[]> findMyCommentRows(@Param("userId") Long userId, Pageable pageable);
}

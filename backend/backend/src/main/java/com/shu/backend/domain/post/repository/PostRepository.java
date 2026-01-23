package com.shu.backend.domain.post.repository;

import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.enums.PostStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Slice<Post> findByBoardId(Long boardId, Pageable pageable);

    /*@Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);*/

    /*// 비관적 락 적용 테스트
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p join fetch p.user u where p.id = :postId")
    Optional<Post> findByIdForUpdate(@Param("postId") Long postId);

    // 낙관적 락 적용 테스트
    @Lock(LockModeType.OPTIMISTIC)
    @Query("select p from Post p join fetch p.user u where p.id = :postId")
    Optional<Post> findByIdForOptimistic(@Param("postId") Long postId);*/

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount + :delta where p.id = :postId")
    int updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.dislikeCount = p.dislikeCount + :delta where p.id = :postId")
    int updateDislikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Query("""
        select p
        from Post p
        join fetch p.user u
        where p.id = :postId
    """)
    Optional<Post> findDetailById(@Param("postId") Long postId);

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

    // 게시글 목록 조회시 필요한 정보만
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
        where p.postStatus = com.shu.backend.domain.post.enums.PostStatus.ACTIVE
          and (
                p.title like concat('%', :keyword, '%')
             or p.content like concat('%', :keyword, '%')
          )
          and (
                (b.scope = com.shu.backend.domain.board.enums.BoardScope.SCHOOL and b.school.id = :schoolId)
             or (b.scope = com.shu.backend.domain.board.enums.BoardScope.REGION and b.region.id = :regionId)
          )
        order by p.id desc
    """)
    List<Object[]> searchAccessiblePostRowsByKeyword(
            @Param("keyword") String keyword,
            @Param("schoolId") Long schoolId,
            @Param("regionId") Long regionId,
            Pageable pageable
    );

    // 검색 조건에 맞는 postId size+1개 조회
    @Query("""
        select p.id
        from Post p
        join p.board b
        where p.postStatus = com.shu.backend.domain.post.enums.PostStatus.ACTIVE
          and (
                p.title like concat('%', :keyword, '%')
             or p.content like concat('%', :keyword, '%')
          )
          and (
                (b.scope = com.shu.backend.domain.board.enums.BoardScope.SCHOOL and b.school.id = :schoolId)
             or (b.scope = com.shu.backend.domain.board.enums.BoardScope.REGION and b.region.id = :regionId)
          )
        order by p.id desc
    """)
    List<Long> findSearchPostIds(
            @Param("keyword") String keyword,
            @Param("schoolId") Long schoolId,
            @Param("regionId") Long regionId,
            Pageable pageable
    );


    // 21개의 postId를 통해 postRow 반환
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
        where p.id in :postIds
        order by p.id desc
    """)
    List<Object[]> findPostRowsByIds(
            @Param("postIds") Collection<Long> postIds
    );
}

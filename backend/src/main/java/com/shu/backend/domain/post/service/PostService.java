package com.shu.backend.domain.post.service;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.exception.BoardException;
import com.shu.backend.domain.board.exception.status.BoardErrorStatus;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.comment.dto.CommentResponse;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.comment.service.CommentQueryService;
import com.shu.backend.domain.post.component.ViewCountAccumulator;
import com.shu.backend.domain.post.dto.PostCreateRequest;
import com.shu.backend.domain.post.dto.PostDetailResponse;
import com.shu.backend.domain.post.dto.PostResponse;
import com.shu.backend.domain.post.dto.PostUpdateRequest;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.enums.PostStatus;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CommentQueryService commentQueryService;

    private final ViewCountAccumulator viewCountAccumulator;

    @PreAuthorize("@penaltyChecker.notPenalized(#userId)")
    @Transactional
    public Long createPost(Long userId, Long boardId, PostCreateRequest req) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BoardErrorStatus.BOARD_NOT_FOUND));

        if (!board.isActive()) {
            throw new BoardException(BoardErrorStatus.BOARD_INACTIVE);
        }

        User user;

        // 현재 board가 지역게시판인 경우
        if (board.getScope() == BoardScope.REGION) {

            user = userRepository.findByIdWithSchoolAndRegion(userId)
                    .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

            Long userRegionId = user.getSchool().getRegion().getId();
            Long boardRegionId = board.getRegion().getId();

            if (!boardRegionId.equals(userRegionId)) {
                throw new PostException(PostErrorStatus.NO_PERMISSION_TO_WRITE);
            }

        }
        // 현재 board가 지역게시판 외의 게시판일 경우
        else if (board.getScope() == BoardScope.SCHOOL) {

            user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

            Long userSchoolId = user.getSchool().getId();
            Long boardSchoolId = board.getSchool().getId();

            if (!boardSchoolId.equals(userSchoolId)) {
                throw new PostException(PostErrorStatus.NO_PERMISSION_TO_WRITE);
            }

        } else {
            throw new BoardException(BoardErrorStatus.INVALID_BOARD_SCOPE);
        }

        String title = req.getTitle().trim();
        String content = req.getContent().trim();

        Post post = Post.builder()
                .title(title)
                .content(content)
                .anonymous(req.isAnonymous())
                .postStatus(PostStatus.ACTIVE)
                .board(board)
                .user(user)
                .build();

        postRepository.save(post);
        return post.getId();
    }

    @PreAuthorize("@penaltyChecker.notPenalized(#userId)")
    @Transactional
    public Long updatePost(Long postId, PostUpdateRequest req, Long userId){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));

        if (post.getPostStatus() == PostStatus.DELETED){
            throw new PostException(PostErrorStatus.POST_ALREADY_DELETED);
        }

        if (!post.getUser().getId().equals(userId)){
            throw new PostException(PostErrorStatus.NO_PERMISSION_TO_WRITE);
        }

        post.update(req.getTitle(), req.getContent(), req.isAnonymous());

        return post.getId();
    }

    @PreAuthorize("@penaltyChecker.notPenalized(#userId)")
    @Transactional
    public Long deletePost(Long postId, Long userId){

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));

        if (post.getPostStatus() == PostStatus.DELETED) {
            throw new PostException(PostErrorStatus.POST_ALREADY_DELETED);
        }

        if (!post.getUser().getId().equals(userId)) {
            throw new PostException(PostErrorStatus.NO_PERMISSION_TO_WRITE);
        }

        post.delete();

        return post.getId();
    }

    // 특정 게시글 상세 조회
    @Transactional(readOnly = true)
    public PostDetailResponse getPostDetail(Long postId) {

        log.info("tx active={}, readOnly={}",
                TransactionSynchronizationManager.isActualTransactionActive(),
                TransactionSynchronizationManager.isCurrentTransactionReadOnly());


        // 원자 업데이트
        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));

        viewCountAccumulator.increment(postId);

        //postRepository.incrementViewCount(postId);

        List<CommentResponse> comments = commentQueryService.getCommentsForPostDetail(postId);

        return PostDetailResponse.toDto(post, comments);
    }

    // 특정 게시판의 글 페이징 조회
    public Slice<PostResponse> getPostsByBoardId(Long boardId, Pageable pageable) {
        // Slice 처리를 위해 size+1로 한 건 더 가져와 hasNext 판정
        Pageable slicePageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize() + 1,
                Sort.by(Sort.Direction.DESC, "id")
        );

        List<Object[]> rows = postRepository.findPostRowsByBoardId(boardId, slicePageable);

        boolean hasNext = rows.size() > pageable.getPageSize();
        if (hasNext) {
            rows = rows.subList(0, pageable.getPageSize());
        }

        List<PostResponse> content = rows.stream()
                .map(PostResponse::fromRow)
                .toList();

        return new SliceImpl<>(content, pageable, hasNext);
    }

    public Slice<PostResponse> searchAccessiblePosts(Long schoolId, Long regionId, String keyword, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            // 빈 키워드 허용 정책은 팀 기준에 맞춰 조정 가능(에러/빈 결과)
            return new SliceImpl<>(List.of(), pageable, false);
        }
        if (schoolId == null || regionId == null) {
            // 인증/프로필 세팅이 안 된 상태를 방어
            return new SliceImpl<>(List.of(), pageable, false);
        }

        Pageable slicePageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize() + 1,
                Sort.by(Sort.Direction.DESC, "id")
        );



        /*List<Object[]> rows = postRepository.searchAccessiblePostRowsByKeyword(
                keyword.trim(),
                schoolId,
                regionId,
                slicePageable
        );*/

        // Phase 1: ID만 가져오기
        List<Long> ids = postRepository.findSearchPostIds(keyword, schoolId, regionId, slicePageable);

        boolean hasNext = ids.size() > pageable.getPageSize();
        if (hasNext) {
            ids = ids.subList(0, pageable.getPageSize());
        }

        if (ids.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }

        // Phase 2: 21개에 대해서만 join + 댓글카운트 수행
        List<Object[]> rows = postRepository.findPostRowsByIds(ids);

        List<PostResponse> content = rows.stream()
                .map(PostResponse::fromRow)
                .toList();

        Pageable responsePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new SliceImpl<>(content, responsePageable, hasNext);
    }

}

package com.shu.backend.domain.post.service;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.exception.BoardException;
import com.shu.backend.domain.board.exception.status.BoardErrorStatus;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.comment.dto.CommentResponse;
import com.shu.backend.domain.comment.service.CommentQueryService;
import com.shu.backend.domain.post.component.ViewCountAccumulator;
import com.shu.backend.domain.post.dto.PostCreateRequest;
import com.shu.backend.domain.post.dto.PostDetailResponse;
import com.shu.backend.domain.post.dto.PostMediaResponse;
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
import org.springframework.web.multipart.MultipartFile;
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

import com.shu.backend.domain.media.entity.Media;
import com.shu.backend.domain.media.enums.MediaTargetType;
import com.shu.backend.domain.media.repository.MediaRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final PostMediaService postMediaService;
    private final MediaRepository mediaRepository;
    private final ViewCountAccumulator viewCountAccumulator;

    @PreAuthorize("@penaltyChecker.notPenalized(#userId)")
    @Transactional
    public Long createPost(Long userId, Long boardId, PostCreateRequest req, List<MultipartFile> files) {

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

        if (files != null && !files.isEmpty()) {
            postMediaService.uploadAndSave(post.getId(), files, user);
        }

        return post.getId();
    }

    @PreAuthorize("@penaltyChecker.notPenalized(#userId)")
    @Transactional
    public Long updatePost(Long postId, PostUpdateRequest req, Long userId, List<MultipartFile> files){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));

        if (post.getPostStatus() == PostStatus.DELETED){
            throw new PostException(PostErrorStatus.POST_ALREADY_DELETED);
        }

        if (!post.getUser().getId().equals(userId)){
            throw new PostException(PostErrorStatus.NO_PERMISSION_TO_WRITE);
        }

        post.update(req.getTitle(), req.getContent(), req.isAnonymous());

        postMediaService.deleteByIds(req.getDeleteMediaIds(), postId, userId);

        if (files != null && !files.isEmpty()) {
            User user = post.getUser();
            postMediaService.uploadAndSave(postId, files, user);
        }

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

        postMediaService.deleteAllByPostId(postId);
        post.delete();

        return post.getId();
    }

    // 특정 게시글 상세 조회
    @Transactional(readOnly = true)
    public PostDetailResponse getPostDetail(Long postId, Long currentUserId) {

        log.info("tx active={}, readOnly={}",
                TransactionSynchronizationManager.isActualTransactionActive(),
                TransactionSynchronizationManager.isCurrentTransactionReadOnly());

        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));

        viewCountAccumulator.increment(postId);

        List<CommentResponse> comments = commentQueryService.getCommentsForPostDetail(postId, currentUserId);
        List<PostMediaResponse> mediaList = postMediaService.getByPostId(postId);

        return PostDetailResponse.toDto(post, comments, mediaList, currentUserId);
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

        content = attachMediaToResponses(content);

        return new SliceImpl<>(content, pageable, hasNext);
    }

    public Slice<PostResponse> searchAccessiblePosts(Long schoolId, Long regionId, String keyword, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            return new SliceImpl<>(List.of(), pageable, false);
        }
        if (keyword.trim().length() > 100) {
            throw new PostException(PostErrorStatus.SEARCH_KEYWORD_TOO_LONG);
        }
        if (schoolId == null) {
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

        content = attachMediaToResponses(content);

        Pageable responsePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new SliceImpl<>(content, responsePageable, hasNext);
    }

    // 최근 3일간 해당 학교의 좋아요 많은 게시글 조회 (이번 주 인기글)
    public List<PostResponse> getHotPosts(Long schoolId, int size) {
        int safeSize = Math.min(Math.max(size, 1), 20);
        LocalDateTime since = LocalDateTime.now().minusDays(3);
        Pageable pageable = PageRequest.of(0, safeSize);
        List<Object[]> rows = postRepository.findHotPostRowsBySchoolId(schoolId, since, pageable);
        List<PostResponse> content = rows.stream().map(PostResponse::fromRow).toList();
        return attachMediaToResponses(content);
    }

    private List<PostResponse> attachMediaToResponses(List<PostResponse> posts) {
        if (posts.isEmpty()) return posts;

        List<Long> postIds = posts.stream().map(PostResponse::getId).toList();
        List<Media> allMedia = mediaRepository.findByTargetTypeAndTargetIdIn(MediaTargetType.POST, postIds);

        Map<Long, List<PostMediaResponse>> mediaByPostId = allMedia.stream()
                .collect(Collectors.groupingBy(
                        Media::getTargetId,
                        Collectors.mapping(PostMediaResponse::from, Collectors.toList())
                ));

        return posts.stream()
                .map(p -> p.withMedia(mediaByPostId.getOrDefault(p.getId(), List.of())))
                .toList();
    }

}

package com.shu.backend.domain.post.service;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.exception.BoardException;
import com.shu.backend.domain.board.exception.status.BoardErrorStatus;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.comment.dto.CommentResponse;
import com.shu.backend.domain.comment.service.CommentQueryService;
import com.shu.backend.domain.bookmark.repository.BookmarkRepository;
import com.shu.backend.domain.poll.dto.PollResponse;
import com.shu.backend.domain.poll.service.PollService;
import com.shu.backend.domain.post.component.ViewCountAccumulator;
import com.shu.backend.global.moderation.ContentModerationService;
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
import com.shu.backend.domain.reaction.entity.Reaction;
import com.shu.backend.domain.reaction.enums.ReactionTargetType;
import com.shu.backend.domain.reaction.repository.ReactionRepository;
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
import org.springframework.web.util.HtmlUtils;

import com.shu.backend.domain.media.entity.Media;
import com.shu.backend.domain.media.enums.MediaTargetType;
import com.shu.backend.domain.media.repository.MediaRepository;
import java.time.LocalDate;
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
    private final ContentModerationService contentModerationService;
    private final BookmarkRepository bookmarkRepository;
    private final PollService pollService;
    private final ReactionRepository reactionRepository;

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

        String rawTitle = req.getTitle().trim();
        String rawContent = req.getContent().trim();
        contentModerationService.checkPost(rawTitle, rawContent);

        String title = HtmlUtils.htmlEscape(rawTitle);
        String content = HtmlUtils.htmlEscape(rawContent);

        Post post = Post.builder()
                .title(title)
                .content(content)
                .anonymous(req.isAnonymous())
                .postStatus(PostStatus.ACTIVE)
                .board(board)
                .user(user)
                .build();

        postRepository.save(post);
        pollService.createPollIfPresent(post, req.getPollOptions());

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

        String rawTitle = req.getTitle().trim();
        String rawContent = req.getContent().trim();
        contentModerationService.checkPost(rawTitle, rawContent);

        String title = HtmlUtils.htmlEscape(rawTitle);
        String content = HtmlUtils.htmlEscape(rawContent);

        post.update(title, content, req.isAnonymous());
        pollService.syncPoll(post, req.getPollOptions());

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
        boolean isBookmarked = bookmarkRepository.existsByUserIdAndPostId(currentUserId, postId);
        PollResponse poll = pollService.getPollResponse(postId, currentUserId);
        Reaction myReaction = reactionRepository
                .findByUserIdAndTargetTypeAndTargetId(currentUserId, ReactionTargetType.POST, postId)
                .orElse(null);
        boolean likedByMe = myReaction != null && Boolean.TRUE.equals(myReaction.getLiked());
        boolean dislikedByMe = myReaction != null && Boolean.TRUE.equals(myReaction.getDisliked());

        return PostDetailResponse.toDto(post, comments, mediaList, currentUserId, isBookmarked, poll, likedByMe, dislikedByMe);
    }

    // 특정 게시판의 글 페이징 조회
    public Slice<PostResponse> getPostsByBoardId(Long boardId, Pageable pageable, Long currentUserId) {
        Sort.Order order = pageable.getSort().stream().findFirst()
                .orElse(Sort.Order.desc("createdAt"));
        String sortBy = order.getProperty();
        String sortDirection = order.getDirection().name();

        // Slice 처리를 위해 size+1로 한 건 더 가져와 hasNext 판정
        Pageable slicePageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize() + 1
        );

        List<Object[]> rows = postRepository.findPostRowsByBoardId(
                boardId,
                currentUserId,
                sortBy,
                sortDirection,
                slicePageable
        );

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

    public Slice<PostResponse> searchAccessiblePosts(Long schoolId, Long regionId, Long boardId, String keyword, Pageable pageable, Long currentUserId) {
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

        // Phase 1: ID만 가져오기 (LIKE 와일드카드 이스케이프)
        String escapedKeyword = keyword.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        List<Long> ids;
        if (boardId != null) {
            Board board = boardRepository.findById(boardId)
                    .orElseThrow(() -> new BoardException(BoardErrorStatus.BOARD_NOT_FOUND));
            if (!isAccessibleBoard(board, schoolId, regionId)) {
                throw new PostException(PostErrorStatus.NO_PERMISSION_TO_WRITE);
            }
            ids = postRepository.findSearchPostIdsByBoardId(escapedKeyword, boardId, currentUserId, slicePageable);
        } else {
            ids = postRepository.findSearchPostIds(escapedKeyword, schoolId, regionId, currentUserId, slicePageable);
        }

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

    private boolean isAccessibleBoard(Board board, Long schoolId, Long regionId) {
        if (board.getScope() == BoardScope.SCHOOL) {
            return board.getSchool() != null && board.getSchool().getId().equals(schoolId);
        }
        if (board.getScope() == BoardScope.REGION) {
            return board.getRegion() != null
                    && regionId != null
                    && board.getRegion().getId().equals(regionId);
        }
        return false;
    }

    // 해당 학교의 HOT 게시글 조회 (filter: TODAY / WEEK / ALL)
    public List<PostResponse> getHotPosts(Long schoolId, String filter, int size, Long currentUserId) {
        int safeSize = Math.min(Math.max(size, 1), 20);
        LocalDateTime since = switch (filter.toUpperCase()) {
            case "TODAY" -> LocalDate.now().atStartOfDay();
            case "ALL"   -> LocalDateTime.of(2020, 1, 1, 0, 0);
            default      -> LocalDateTime.now().minusDays(7);  // WEEK
        };
        Pageable pageable = PageRequest.of(0, safeSize);
        List<Object[]> rows = postRepository.findHotPostRowsBySchoolId(schoolId, since, currentUserId, pageable);
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

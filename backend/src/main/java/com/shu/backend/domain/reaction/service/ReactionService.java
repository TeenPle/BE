package com.shu.backend.domain.reaction.service;

import com.shu.backend.domain.board.service.BoardAccessPolicy;
import com.shu.backend.domain.comment.exception.CommentException;
import com.shu.backend.domain.comment.exception.status.CommentErrorStatus;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.domain.notification.service.NotificationService;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.push.service.PushService;
import com.shu.backend.domain.reaction.dto.ReactionApplyRequest;
import com.shu.backend.domain.reaction.dto.ReactionApplyResponse;
import com.shu.backend.domain.reaction.entity.Reaction;
import com.shu.backend.domain.reaction.enums.ReactionAction;
import com.shu.backend.domain.reaction.enums.ReactionTargetType;
import com.shu.backend.domain.reaction.exception.ReactionException;
import com.shu.backend.domain.reaction.exception.status.ReactionErrorStatus;
import com.shu.backend.domain.reaction.repository.ReactionRepository;
import com.shu.backend.domain.usersetting.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final PushService pushService;
    private final UserSettingRepository userSettingRepository;
    private final BoardAccessPolicy boardAccessPolicy;

    @PreAuthorize("@penaltyChecker.notPenalized(#userId)")
    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public ReactionApplyResponse apply(Long userId, ReactionApplyRequest req) {
        validateTargetAccessible(userId, req.getTargetType(), req.getTargetId());

        try {
            return doApply(userId, req);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 unique 제약 충돌 발생: noRollbackFor로 트랜잭션을 살려두고
            // 이미 저장된 레코드를 조회해 재처리한다.
            return doApply(userId, req);
        }
    }

    private ReactionApplyResponse doApply(Long userId, ReactionApplyRequest req) {
        ReactionTargetType targetType = req.getTargetType();
        Long targetId = req.getTargetId();
        ReactionAction action = req.getAction();

        // 해당 게시글/댓글에 대한 유저의 기존 reaction이 있으면 조회, 없으면 생성
        Reaction reaction = reactionRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .orElseGet(() -> reactionRepository.save(Reaction.create(targetType, targetId, userId)));

        boolean wasLiked = Boolean.TRUE.equals(reaction.getLiked());
        boolean wasDisliked = Boolean.TRUE.equals(reaction.getDisliked());
        boolean changed;
        if (action == ReactionAction.LIKE) {
            changed = reaction.applyLike();
        } else { // DISLIKE
            changed = reaction.applyDislike();
        }

        int likeDelta = Boolean.compare(Boolean.TRUE.equals(reaction.getLiked()), wasLiked);
        int dislikeDelta = Boolean.compare(Boolean.TRUE.equals(reaction.getDisliked()), wasDisliked);
        if (changed && (likeDelta != 0 || dislikeDelta != 0)) {
            increment(targetType, targetId, likeDelta, dislikeDelta);
        }

        int likeCount;
        int dislikeCount;
        Long ownerUserId;
        String boardName;

        if (targetType == ReactionTargetType.COMMENT) {
            var c = commentRepository.findById(targetId)
                    .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND));
            likeCount = c.getLikeCount();
            dislikeCount = c.getDislikeCount();
            ownerUserId = c.getUser().getId();
            boardName = c.getPost().getBoard().getTitle();
        } else if (targetType == ReactionTargetType.POST) {
            var p = postRepository.findById(targetId)
                    .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));
            likeCount = p.getLikeCount();
            dislikeCount = p.getDislikeCount();
            ownerUserId = p.getUser().getId();
            boardName = p.getBoard().getTitle();
        } else {
            throw new ReactionException(ReactionErrorStatus.UNSUPPORTED_TARGET_TYPE);
        }

        // 좋아요가 새로 적용됐고, 자신의 글/댓글이 아니며, 좋아요 수가 10의 배수일 때만 알림 발송
        if (changed && action == ReactionAction.LIKE && !ownerUserId.equals(userId)
                && likeCount > 0 && likeCount % 10 == 0) {

            NotificationType notiType = (targetType == ReactionTargetType.POST)
                    ? NotificationType.POST_LIKE : NotificationType.COMMENT_LIKE;
            NotificationTargetType notiTargetType = (targetType == ReactionTargetType.POST)
                    ? NotificationTargetType.POST : NotificationTargetType.COMMENT;
            String targetLabel = (targetType == ReactionTargetType.POST) ? "게시글" : "댓글";
            // 서비스 전반에서 '공감' 대신 '좋아요'로 용어를 통일한다. (FE 설정 화면의 '좋아요 알림'과 일치)
            String notiMsg = "내 " + targetLabel + "에 좋아요가 " + likeCount + "개 모였어요!";

            Long notificationId = notificationService.create(
                    notiType,
                    notiTargetType,
                    targetId,
                    notiMsg,
                    ownerUserId,
                    userId,
                    boardName
            );

            if (notificationId != null) {
                var setting = userSettingRepository.findByUserId(ownerUserId).orElse(null);
                if (setting == null || setting.isLikeNotificationEnabled()) {
                    try {
                        pushService.sendToUserAfterCommit(
                                ownerUserId,
                                boardName,
                                notiMsg,
                                Map.of(
                                        "notificationId", String.valueOf(notificationId),
                                        "type", notiType.name(),
                                        "targetType", notiTargetType.name(),
                                        "targetId", String.valueOf(targetId)
                                )
                        );
                    } catch (Exception e) {
                        log.warn("Reaction push scheduling failed: notificationId={}, receiverUserId={}, targetType={}, targetId={}",
                                notificationId, ownerUserId, targetType, targetId, e);
                    }
                }
            }
        }

        return ReactionApplyResponse.builder()
                .targetId(targetId)
                .targetType(targetType.name())
                .liked(reaction.getLiked())
                .disliked(reaction.getDisliked())
                .applied(changed)
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .build();

    }

    private void validateTargetAccessible(Long userId, ReactionTargetType targetType, Long targetId) {
        if (targetType == ReactionTargetType.COMMENT) {
            var comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND));
            boardAccessPolicy.assertCanAccessComment(userId, comment);
            return;
        }
        if (targetType == ReactionTargetType.POST) {
            var post = postRepository.findById(targetId)
                    .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));
            boardAccessPolicy.assertCanAccessPost(userId, post);
            return;
        }
        throw new ReactionException(ReactionErrorStatus.UNSUPPORTED_TARGET_TYPE);

    }

    private void increment(ReactionTargetType targetType, Long targetId, int likeDelta, int dislikeDelta) {
        if (targetType == ReactionTargetType.COMMENT) {
            if (likeDelta != 0) commentRepository.updateLikeCount(targetId, likeDelta);
            if (dislikeDelta != 0) commentRepository.updateDislikeCount(targetId, dislikeDelta);
            return;
        }
        if (targetType == ReactionTargetType.POST) {
            if (likeDelta != 0) postRepository.updateLikeCount(targetId, likeDelta);
            if (dislikeDelta != 0) postRepository.updateDislikeCount(targetId, dislikeDelta);
            return;
        }
        throw new ReactionException(ReactionErrorStatus.UNSUPPORTED_TARGET_TYPE);
    }
}

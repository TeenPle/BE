package com.shu.backend.domain.reaction.service;

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

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final PushService pushService;
    private final UserSettingRepository userSettingRepository;

    @PreAuthorize("@penaltyChecker.notPenalized(#userId)")
    @Transactional
    public ReactionApplyResponse apply(Long userId, ReactionApplyRequest req) {
        validateTargetExists(req.getTargetType(), req.getTargetId());

        try {
            return doApply(userId, req);
        } catch (DataIntegrityViolationException e) {
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

        boolean changed;    //클릭으로 실제 반영 되었는지 중복 클릭이었는지 여부를 응답에 포함시키기 위한 변수
        if (action == ReactionAction.LIKE) {
            changed = reaction.applyLike(); // false->true일 때만 true
            if (changed) {
                increment(targetType, targetId, +1, 0);


            }
        } else { // DISLIKE
            changed = reaction.applyDislike();
            if (changed) {
                increment(targetType, targetId, 0, +1);
            }
        }

        int likeCount;
        int dislikeCount;
        Long ownerUserId;

        if (targetType == ReactionTargetType.COMMENT) {
            var c = commentRepository.findById(targetId)
                    .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND));
            likeCount = c.getLikeCount();
            dislikeCount = c.getDislikeCount();
            ownerUserId = c.getUser().getId();
        } else if (targetType == ReactionTargetType.POST) {
            var p = postRepository.findById(targetId)
                    .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));
            likeCount = p.getLikeCount();
            dislikeCount = p.getDislikeCount();
            ownerUserId = p.getUser().getId();
        } else {
            throw new ReactionException(ReactionErrorStatus.UNSUPPORTED_TARGET_TYPE);
        }

        // 좋아요가 새로 적용됐고, 자신의 글/댓글이 아닌 경우에만 알림 발송
        if (changed && action == ReactionAction.LIKE && !ownerUserId.equals(userId)) {
            NotificationType notiType = (targetType == ReactionTargetType.POST)
                    ? NotificationType.POST_LIKE : NotificationType.COMMENT_LIKE;
            NotificationTargetType notiTargetType = (targetType == ReactionTargetType.POST)
                    ? NotificationTargetType.POST : NotificationTargetType.COMMENT;
            String notiMsg = (targetType == ReactionTargetType.POST)
                    ? "내 글에 좋아요가 눌렸습니다." : "내 댓글에 좋아요가 눌렸습니다.";

            Long notificationId = notificationService.create(
                    notiType,
                    notiTargetType,
                    targetId,
                    notiMsg,
                    ownerUserId,
                    userId
            );

            if (notificationId != null) {
                var setting = userSettingRepository.findByUserId(ownerUserId).orElse(null);
                if (setting != null && setting.isLikeNotificationEnabled()) {
                    try {
                        pushService.sendToUser(
                                ownerUserId,
                                "새 좋아요",
                                notiMsg,
                                Map.of(
                                        "notificationId", String.valueOf(notificationId),
                                        "type", notiType.name(),
                                        "targetType", notiTargetType.name(),
                                        "targetId", String.valueOf(targetId)
                                )
                        );
                    } catch (Exception ignore) {}
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

    private void validateTargetExists(ReactionTargetType targetType, Long targetId) {
        if (targetType == ReactionTargetType.COMMENT) {
            if (!commentRepository.existsById(targetId)) throw new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND);
            return;
        }
        if (targetType == ReactionTargetType.POST) {
            if (!postRepository.existsById(targetId)) throw new PostException(PostErrorStatus.POST_NOT_FOUND);
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

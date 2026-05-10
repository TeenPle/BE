package com.shu.backend.domain.post.component;

import com.shu.backend.domain.bookmark.repository.BookmarkRepository;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.media.enums.MediaTargetType;
import com.shu.backend.domain.media.repository.MediaRepository;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.repository.NotificationRepository;
import com.shu.backend.domain.poll.repository.PollOptionRepository;
import com.shu.backend.domain.poll.repository.PollRepository;
import com.shu.backend.domain.poll.repository.PollVoteRepository;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.reaction.enums.ReactionTargetType;
import com.shu.backend.domain.reaction.repository.ReactionRepository;
import com.shu.backend.domain.report.enums.TargetType;
import com.shu.backend.domain.report.repository.ReportRepository;
import com.shu.backend.global.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostPurgeService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final MediaRepository mediaRepository;
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public void purgePost(Long postId) {
        // 1. 댓글 ID 수집 (댓글 반응/알림/신고 삭제에 필요)
        List<Long> commentIds = commentRepository.findIdsByPostId(postId);

        if (!commentIds.isEmpty()) {
            // 2. 댓글 반응 삭제
            reactionRepository.deleteAllByTargetTypeAndTargetIdIn(ReactionTargetType.COMMENT, commentIds);
            // 3. 댓글 알림 삭제
            notificationRepository.deleteAllByTargetTypeAndTargetIdIn(NotificationTargetType.COMMENT, commentIds);
            // 4. 댓글 신고 삭제
            reportRepository.deleteAllByTargetTypeAndTargetIdIn(TargetType.COMMENT, commentIds);
        }

        // 5. 댓글 삭제: parent_id FK 위반 방지를 위해 parent 참조를 먼저 null로 끊은 뒤 일괄 삭제
        commentRepository.nullifyParentByPostId(postId);
        commentRepository.deleteAllByPostId(postId);

        // 6. 투표(Poll) 삭제: PollVote → PollOption → Poll
        pollRepository.findByPostId(postId).ifPresent(poll -> {
            pollVoteRepository.deleteByPollId(poll.getId());
            pollOptionRepository.deleteByPollId(poll.getId());
            pollRepository.delete(poll);
        });

        // 7. 게시글 반응 삭제
        reactionRepository.deleteAllByTargetTypeAndTargetId(ReactionTargetType.POST, postId);

        // 8. 북마크 삭제
        bookmarkRepository.deleteAllByPostId(postId);

        // 9. 미디어 삭제 (deletePost 시 이미 삭제되었을 수 있으나 안전하게 재확인)
        List<com.shu.backend.domain.media.entity.Media> mediaList =
                mediaRepository.findByTargetTypeAndTargetId(MediaTargetType.POST, postId);
        for (com.shu.backend.domain.media.entity.Media media : mediaList) {
            try {
                String target = media.getS3Key() != null ? media.getS3Key() : media.getUrl();
                fileStorageService.deletePublicFile(target);
            } catch (Exception e) {
                log.warn("[PostPurge] S3 삭제 실패 (postId={}, mediaId={}): {}", postId, media.getId(), e.getMessage());
            }
        }
        if (!mediaList.isEmpty()) {
            mediaRepository.deleteAll(mediaList);
        }

        // 10. 게시글 알림 삭제
        notificationRepository.deleteAllByTargetTypeAndTargetId(NotificationTargetType.POST, postId);

        // 11. 게시글 신고 삭제
        reportRepository.deleteAllByTargetTypeAndTargetId(TargetType.POST, postId);

        // 12. 게시글 hard-delete
        postRepository.deleteById(postId);

        log.debug("[PostPurge] Purged post {}", postId);
    }
}

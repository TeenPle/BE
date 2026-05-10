package com.shu.backend.domain.post.component;

import com.shu.backend.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostPurgeScheduler {

    private static final int PURGE_DAYS = 7;
    private static final int BATCH_SIZE = 100;

    private final PostRepository postRepository;
    private final PostPurgeService postPurgeService;

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeDeletedPosts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(PURGE_DAYS);
        List<Long> postIds = postRepository.findPurgeablePostIds(threshold, PageRequest.of(0, BATCH_SIZE));

        if (postIds.isEmpty()) {
            return;
        }

        log.info("[PostPurge] {} posts queued for hard-delete (deleted before {})", postIds.size(), threshold);

        int success = 0;
        for (Long postId : postIds) {
            try {
                postPurgeService.purgePost(postId);
                success++;
            } catch (Exception e) {
                log.error("[PostPurge] Failed to purge post {}: {}", postId, e.getMessage());
            }
        }

        log.info("[PostPurge] Completed. {}/{} posts purged.", success, postIds.size());
    }
}

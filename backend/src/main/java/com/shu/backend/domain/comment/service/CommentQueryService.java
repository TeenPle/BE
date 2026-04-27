package com.shu.backend.domain.comment.service;

import com.shu.backend.domain.comment.dto.CommentResponse;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.enums.CommentStatus;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.reaction.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;

    public List<CommentResponse> getCommentsForPostDetail(Long postId, Long currentUserId) {
        List<Comment> parents = commentRepository.findParentsForPostDetail(postId);

        if (parents.isEmpty()) {
            return List.of();
        }

        List<Long> parentIds = parents.stream().map(Comment::getId).toList();
        List<Comment> children = commentRepository.findChildrenByParentIds(parentIds);

        Map<Long, List<Comment>> childrenMap = children.stream()
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId()));

        // Collect all visible comments for batch reaction lookup and anonymous numbering
        List<Comment> allVisible = new ArrayList<>();
        Map<Long, List<Comment>> parentToVisibleReplies = new LinkedHashMap<>();

        for (Comment parent : parents) {
            List<Comment> allReplies = childrenMap.getOrDefault(parent.getId(), List.of());
            List<Comment> visibleReplies = allReplies.stream()
                    .filter(r -> r.getCommentStatus() != CommentStatus.DELETED)
                    .toList();

            if (parent.getCommentStatus() == CommentStatus.DELETED && visibleReplies.isEmpty()) {
                continue;
            }

            allVisible.add(parent);
            allVisible.addAll(visibleReplies);
            parentToVisibleReplies.put(parent.getId(), visibleReplies);
        }

        // Batch fetch liked comment IDs for current user
        Set<Long> likedCommentIds = Set.of();
        if (currentUserId != null && !allVisible.isEmpty()) {
            List<Long> allCommentIds = allVisible.stream().map(Comment::getId).toList();
            likedCommentIds = reactionRepository.findLikedCommentIds(currentUserId, allCommentIds);
        }

        // Build anonymous numbering: same user gets same number within this post
        // Post author (if anonymous) gets number 0 treated as special — here we just assign sequential numbers
        Map<Long, Integer> userAnonNumberMap = new LinkedHashMap<>();

        // Flatten in display order to assign numbers in order of first appearance
        for (Comment c : allVisible) {
            if (c.getAnonymous() && c.getUser() != null) {
                userAnonNumberMap.computeIfAbsent(c.getUser().getId(), k -> userAnonNumberMap.size() + 1);
            }
        }

        // Build final response list
        List<CommentResponse> result = new ArrayList<>();

        for (Comment parent : parents) {
            List<Comment> visibleReplies = parentToVisibleReplies.get(parent.getId());
            if (visibleReplies == null) continue; // was excluded above

            result.add(toDto(parent, currentUserId, likedCommentIds, userAnonNumberMap));
            for (Comment reply : visibleReplies) {
                result.add(toDto(reply, currentUserId, likedCommentIds, userAnonNumberMap));
            }
        }

        return result;
    }

    private CommentResponse toDto(Comment comment, Long currentUserId, Set<Long> likedCommentIds, Map<Long, Integer> userAnonNumberMap) {
        boolean likedByMe = likedCommentIds.contains(comment.getId());
        int anonNumber = 0;
        if (comment.getAnonymous() && comment.getUser() != null) {
            anonNumber = userAnonNumberMap.getOrDefault(comment.getUser().getId(), 0);
        }
        return CommentResponse.toDto(comment, currentUserId, likedByMe, anonNumber);
    }
}

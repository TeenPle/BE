package com.shu.backend.domain.comment.service;

import com.shu.backend.domain.boardprofile.entity.BoardDisplayProfile;
import com.shu.backend.domain.boardprofile.service.BoardDisplayProfileService;
import com.shu.backend.domain.comment.dto.CommentResponse;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.enums.CommentStatus;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.reaction.repository.ReactionRepository;
import com.shu.backend.domain.user.enums.UserRole;
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
    private final BoardDisplayProfileService boardDisplayProfileService;

    public List<CommentResponse> getCommentsForPostDetail(Long postId, Long currentUserId, Long postAuthorId, Long boardId) {
        List<Comment> parents = commentRepository.findParentsForPostDetail(postId, currentUserId);

        if (parents.isEmpty()) {
            return List.of();
        }

        List<Long> parentIds = parents.stream().map(Comment::getId).toList();
        List<Comment> children = commentRepository.findChildrenByParentIds(parentIds, currentUserId);

        Map<Long, List<Comment>> childrenMap = children.stream()
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId()));

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

        Set<Long> likedCommentIds = Set.of();
        if (currentUserId != null && !allVisible.isEmpty()) {
            List<Long> allCommentIds = allVisible.stream().map(Comment::getId).toList();
            likedCommentIds = reactionRepository.findLikedCommentIds(currentUserId, allCommentIds);
        }

        Map<Long, BoardDisplayProfile> profilesByUserId = boardDisplayProfileService.getOrCreateByUserIds(
                boardId,
                allVisible.stream()
                        .map(Comment::getUser)
                        .filter(Objects::nonNull)
                        .filter(user -> user.getRole() != UserRole.ADMIN)
                        .map(User -> User.getId())
                        .toList()
        );

        List<CommentResponse> result = new ArrayList<>();

        for (Comment parent : parents) {
            List<Comment> visibleReplies = parentToVisibleReplies.get(parent.getId());
            if (visibleReplies == null) continue;

            result.add(toDto(parent, currentUserId, postAuthorId, likedCommentIds, profilesByUserId));
            for (Comment reply : visibleReplies) {
                result.add(toDto(reply, currentUserId, postAuthorId, likedCommentIds, profilesByUserId));
            }
        }

        return result;
    }

    private CommentResponse toDto(Comment comment, Long currentUserId, Long postAuthorId, Set<Long> likedCommentIds, Map<Long, BoardDisplayProfile> profilesByUserId) {
        boolean likedByMe = likedCommentIds.contains(comment.getId());
        BoardDisplayProfile profile = comment.getUser() == null ? null : profilesByUserId.get(comment.getUser().getId());
        String profileImageUrl = profile == null ? null : boardDisplayProfileService.toReadUrl(profile.getProfileImageUrl());
        return CommentResponse.toDto(comment, currentUserId, postAuthorId, likedByMe, profile, profileImageUrl);
    }
}

package com.shu.backend.domain.comment.service;

import com.shu.backend.domain.comment.dto.CommentResponse;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final CommentRepository commentRepository;

    /// 게시글 상세용 댓글 목록 조회 (부모 댓글 + 대댓글 포함)
    public List<CommentResponse> getCommentsForPostDetail(Long postId) {
        // 1. 부모 댓글 먼저 조회
        List<Comment> parents = commentRepository.findParentsForPostDetail(postId);

        if (parents.isEmpty()) {
            return List.of();
        }

        // 2. 부모 댓글 ID 목록 추출
        List<Long> parentIds = parents.stream()
                .map(Comment::getId)
                .toList();

        // 3. 대댓글 한 번에 조회
        List<Comment> children = commentRepository.findChildrenByParentIds(parentIds);

        // 4. 부모 ID 기준으로 대댓글 그룹핑
        Map<Long, List<Comment>> childrenMap = children.stream()
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId()));

        // 5. 부모 댓글 뒤에 대댓글이 이어지는 평면 리스트 구성
        List<CommentResponse> result = new ArrayList<>();

        for (Comment parent : parents) {
            result.add(CommentResponse.toDto(parent));

            List<Comment> replies = childrenMap.getOrDefault(parent.getId(), List.of());
            for (Comment reply : replies) {
                result.add(CommentResponse.toDto(reply));
            }
        }

        return result;
    }
}
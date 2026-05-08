package com.shu.backend.domain.bookmark.service;

import com.shu.backend.domain.board.service.BoardAccessPolicy;
import com.shu.backend.domain.bookmark.dto.BookmarkResult;
import com.shu.backend.domain.bookmark.dto.BookmarkedPostResponse;
import com.shu.backend.domain.bookmark.entity.Bookmark;
import com.shu.backend.domain.bookmark.repository.BookmarkRepository;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.util.PageRequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final BoardAccessPolicy boardAccessPolicy;

    @Transactional
    public BookmarkResult toggle(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));
        boardAccessPolicy.assertCanAccessPost(userId, post);

        Optional<Bookmark> existing = bookmarkRepository.findByUserIdAndPostId(userId, postId);

        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return new BookmarkResult(false);
        }

        User user = boardAccessPolicy.requireActiveUserWithSchool(userId);

        bookmarkRepository.save(Bookmark.builder()
                .user(user)
                .post(post)
                .build());

        return new BookmarkResult(true);
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));
        boardAccessPolicy.assertCanAccessPost(userId, post);
        return bookmarkRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Transactional(readOnly = true)
    public List<BookmarkedPostResponse> getMyBookmarks(Long userId, int page, int size) {
        User user = boardAccessPolicy.requireActiveUserWithSchool(userId);
        Long schoolId = user.getSchool().getId();
        Long regionId = user.getSchool().getRegion() != null ? user.getSchool().getRegion().getId() : null;
        List<Bookmark> bookmarks = bookmarkRepository
                .findAccessibleByUserIdOrderByCreatedAtDesc(
                        userId,
                        schoolId,
                        regionId,
                        PageRequestUtils.of(page, size)
                );

        return bookmarks.stream()
                .map(b -> BookmarkedPostResponse.from(b, b.getPost().getCommentCount()))
                .collect(Collectors.toList());
    }
}

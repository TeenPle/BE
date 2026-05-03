package com.shu.backend.domain.bookmark.service;

import com.shu.backend.domain.bookmark.dto.BookmarkResult;
import com.shu.backend.domain.bookmark.dto.BookmarkedPostResponse;
import com.shu.backend.domain.bookmark.entity.Bookmark;
import com.shu.backend.domain.bookmark.repository.BookmarkRepository;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public BookmarkResult toggle(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));

        Optional<Bookmark> existing = bookmarkRepository.findByUserIdAndPostId(userId, postId);

        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return new BookmarkResult(false);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        bookmarkRepository.save(Bookmark.builder()
                .user(user)
                .post(post)
                .build());

        return new BookmarkResult(true);
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long postId) {
        return bookmarkRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Transactional(readOnly = true)
    public List<BookmarkedPostResponse> getMyBookmarks(Long userId, int page, int size) {
        List<Bookmark> bookmarks = bookmarkRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        return bookmarks.stream()
                .map(b -> {
                    int commentCount = commentRepository.countByPostId(b.getPost().getId());
                    return BookmarkedPostResponse.from(b, commentCount);
                })
                .collect(Collectors.toList());
    }
}

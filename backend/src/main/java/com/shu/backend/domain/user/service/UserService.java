package com.shu.backend.domain.user.service;

import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.user.dto.UserDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDTO.ProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findByIdWithSchoolAndRegion(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        return UserDTO.ProfileResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .schoolName(user.getSchool().getName())
                .grade(user.getGrade())
                .gender(user.getGender())
                .verified(user.isVerified())
                .phoneVerified(user.isPhoneVerified())
                .build();
    }

    @Transactional
    public void updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        if (user.getNickname().equals(nickname)) {
            throw new GeneralException(UserErrorStatus.SAME_NICKNAME);
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new GeneralException(UserErrorStatus.EXIST_NICKNAME);
        }

        user.updateNickname(nickname);
    }

    @Transactional
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new GeneralException(UserErrorStatus.INVALID_PASSWORD);
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new GeneralException(UserErrorStatus.SAME_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    @Transactional(readOnly = true)
    public List<UserDTO.MyPostResponse> getMyPosts(Long userId, int page, int size) {
        List<Object[]> rows = postRepository.findMyPostRows(userId, PageRequest.of(page, size));
        return rows.stream().map(r -> UserDTO.MyPostResponse.builder()
                .postId((Long) r[0])
                .title((String) r[1])
                .content((String) r[2])
                .postStatus(r[3] instanceof Enum<?> e ? e.name() : String.valueOf(r[3]))
                .likeCount(r[4] == null ? 0 : ((Number) r[4]).intValue())
                .createdAt((LocalDateTime) r[5])
                .commentCount(r[6] == null ? 0 : ((Number) r[6]).intValue())
                .build()
        ).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDTO.MyCommentResponse> getMyComments(Long userId, int page, int size) {
        List<Object[]> rows = commentRepository.findMyCommentRows(userId, PageRequest.of(page, size));
        return rows.stream().map(r -> UserDTO.MyCommentResponse.builder()
                .commentId((Long) r[0])
                .content((String) r[1])
                .postId((Long) r[2])
                .postTitle((String) r[3])
                .likeCount(r[4] == null ? 0 : ((Number) r[4]).intValue())
                .createdAt((LocalDateTime) r[5])
                .build()
        ).toList();
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));
        user.deactivate();
    }
}

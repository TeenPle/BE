package com.shu.backend.domain.user.service;

import com.shu.backend.domain.block.repository.UserBlockRepository;
import com.shu.backend.domain.bookmark.repository.BookmarkRepository;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.notification.repository.NotificationRepository;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.pushtoken.repository.PushTokenRepository;
import com.shu.backend.domain.reaction.repository.ReactionRepository;
import com.shu.backend.domain.user.dto.UserDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.RefreshTokenRepository;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.verification.entity.UserSchoolVerificationRequest;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRepository;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRequestRepository;
import com.shu.backend.global.exception.GeneralException;
import com.shu.backend.global.file.FileStorageService;
import com.shu.backend.global.util.PageRequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PushTokenRepository pushTokenRepository;
    private final BookmarkRepository bookmarkRepository;
    private final NotificationRepository notificationRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserSchoolVerificationRequestRepository verificationRequestRepository;
    private final UserSchoolVerificationRepository verificationRepository;

    @Transactional(readOnly = true)
    public UserDTO.ProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findByIdWithSchoolAndRegion(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        long myPostCount = postRepository.countActiveByUserId(userId);
        long myCommentCount = commentRepository.countActiveByUserId(userId);

        return UserDTO.ProfileResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(fileStorageService.toPresignedReadUrl(user.getProfileImageUrl()))
                .schoolName(user.getSchool().getName())
                .grade(user.getGrade())
                .gender(user.getGender())
                .verified(user.isVerified())
                .phoneVerified(user.isPhoneVerified())
                .myPostCount(myPostCount)
                .myCommentCount(myCommentCount)
                .nicknameChangedAt(user.getNicknameChangedAt())
                .build();
    }

    @Transactional
    public void updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        if (user.getNickname().equals(nickname)) {
            throw new GeneralException(UserErrorStatus.SAME_NICKNAME);
        }
        if (user.getNicknameChangedAt() != null &&
                user.getNicknameChangedAt().isAfter(LocalDateTime.now().minusDays(30))) {
            throw new GeneralException(UserErrorStatus.NICKNAME_CHANGE_COOLDOWN);
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
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional(readOnly = true)
    public List<UserDTO.MyPostResponse> getMyPosts(Long userId, int page, int size) {
        List<Object[]> rows = postRepository.findMyPostRows(userId, PageRequestUtils.of(page, size));
        return rows.stream().map(r -> UserDTO.MyPostResponse.builder()
                .postId((Long) r[0])
                .title((String) r[1])
                .content((String) r[2])
                .postStatus(r[3] instanceof Enum<?> e ? e.name() : String.valueOf(r[3]))
                .likeCount(r[4] == null ? 0 : ((Number) r[4]).intValue())
                .createdAt((LocalDateTime) r[5])
                .commentCount(r[6] == null ? 0 : ((Number) r[6]).intValue())
                .boardTitle((String) r[7])
                .build()
        ).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDTO.MyCommentResponse> getMyComments(Long userId, int page, int size) {
        List<Object[]> rows = commentRepository.findMyCommentRows(userId, PageRequestUtils.of(page, size));
        return rows.stream().map(r -> UserDTO.MyCommentResponse.builder()
                .commentId((Long) r[0])
                .content((String) r[1])
                .postId((Long) r[2])
                .postTitle((String) r[3])
                .likeCount(r[4] == null ? 0 : ((Number) r[4]).intValue())
                .createdAt((LocalDateTime) r[5])
                .boardTitle((String) r[6])
                .build()
        ).toList();
    }

    @Transactional
    public String updateProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));
        String oldUrl = user.getProfileImageUrl();
        String imageUrl = fileStorageService.uploadProfileImage(file);
        user.updateProfileImageUrl(imageUrl);
        if (oldUrl != null && oldUrl.startsWith("http")) {
            fileStorageService.deletePublicFile(oldUrl);
        }
        return fileStorageService.toPresignedReadUrl(imageUrl);
    }

    @Transactional(readOnly = true)
    public List<UserDTO.MyPostResponse> getLikedPosts(Long userId, int page, int size) {
        List<Long> postIds = reactionRepository.findLikedPostIds(userId, PageRequestUtils.of(page, size));
        if (postIds.isEmpty()) return Collections.emptyList();

        List<Object[]> rows = postRepository.findLikedPostRows(postIds);
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

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        // 1. 학생증 인증 이미지 S3(프라이빗) 삭제 후 인증 요청 레코드 삭제
        List<UserSchoolVerificationRequest> verifications = verificationRequestRepository.findByUser(user);
        for (UserSchoolVerificationRequest v : verifications) {
            try {
                fileStorageService.deleteStudentCardImage(v.getRequestImageUrl());
            } catch (Exception e) {
                log.warn("[Withdrawal] 학생증 S3 삭제 실패 (userId={}, url={}): {}", userId, v.getRequestImageUrl(), e.getMessage());
            }
        }
        verificationRequestRepository.deleteByUser(user);

        // 1-2. 승인된 학교 인증 기록 삭제
        verificationRepository.deleteByUserId(userId);

        // 2. 프로필 이미지 S3(퍼블릭) 삭제
        String profileUrl = user.getProfileImageUrl();
        if (profileUrl != null && profileUrl.startsWith("http")) {
            try {
                fileStorageService.deletePublicFile(profileUrl);
            } catch (Exception e) {
                log.warn("[Withdrawal] 프로필 이미지 S3 삭제 실패 (userId={}): {}", userId, e.getMessage());
            }
        }

        // 3. 유저 행동 기록 삭제
        bookmarkRepository.deleteAllByUserId(userId);
        reactionRepository.deleteAllByUserId(userId);
        notificationRepository.deleteAllByUserId(userId);
        userBlockRepository.deleteByBlockerId(userId);   // 내가 차단한 목록
        userBlockRepository.deleteByBlockedId(userId);   // 나를 차단한 목록
        refreshTokenRepository.deleteByUser(user);
        pushTokenRepository.deleteAllByUserId(userId);

        // 4. PII 즉시 익명화 (게시글/댓글 FK 보존을 위해 행은 유지)
        user.anonymize();
        userRepository.save(user);
    }
}

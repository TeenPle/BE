package com.shu.backend.domain.user.service;

import com.shu.backend.domain.auth.service.VerificationService;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.repository.SchoolRepository;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.user.dto.*;
import com.shu.backend.domain.user.entity.RefreshToken;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.RefreshTokenRepository;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.usersetting.entity.UserSetting;
import com.shu.backend.domain.usersetting.repository.UserSettingRepository;
import com.shu.backend.domain.verification.entity.UserSchoolVerificationRequest;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRequestRepository;
import com.shu.backend.domain.verification.status.VerificationStatus;
import com.shu.backend.global.jwt.JwtProperties;
import com.shu.backend.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserSchoolVerificationRequestRepository verificationRequestRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final VerificationService smsVerificationService;

    private static final String ADMIN_SCHOOL_NAME = "운영자전용학교";

    // 회원가입
    public SignUpResponseDTO join(UserRequestDTO.SignUp request, String studentIdImageUrl) {
        validateSignUpRequest(request);

        smsVerificationService.verifyTokenOrThrow(
                request.getVerificationToken(),
                request.getEmail()
        );

        School school = getSchoolByName(request.getSchool());
        User newUser = createUser(request, school, true);
        userRepository.save(newUser);
        // 회원가입 시 알림 설정 기본값으로 자동 생성 (없으면 푸시 발송 조건에서 누락됨)
        userSettingRepository.save(UserSetting.create(newUser));
        createVerificationRequest(newUser, school, studentIdImageUrl);

        String accessToken = jwtTokenProvider.createAccessToken(newUser.getId());
        String refreshToken = issueRefreshToken(newUser);

        return new SignUpResponseDTO(newUser.getId(), accessToken, refreshToken);
    }

    // 로그인
    @Transactional
    public LoginResponseDTO login(UserLoginDTO userLoginDTO) {
        User user = userRepository.findByEmail(userLoginDTO.getEmail())
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));

        if (!passwordEncoder.matches(userLoginDTO.getPassword(), user.getPassword())) {
            throw new UserException(UserErrorStatus.INVALID_PASSWORD);
        }

        if (user.getRole() == UserRole.USER) {
            if (user.isVerified()) {
                return buildLoginResponse(user);
            }

            UserSchoolVerificationRequest latestRequest =
                    verificationRequestRepository.findTopByUserOrderByRequestedAtDesc(user)
                            .orElse(null);

            if (latestRequest == null) {
                throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_REQUIRED);
            }

            switch (latestRequest.getStatus()) {
                case PENDING -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_PENDING);
                case REJECTED -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_REJECTED);
                case APPROVED -> { /* 통과 */ }
                default -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_STATUS_INVALID);
            }
        }

        return buildLoginResponse(user);
    }

    // Refresh token으로 새 토큰 발급 (rotation)
    @Transactional
    public TokenRefreshResponseDTO refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new UserException(UserErrorStatus.INVALID_REFRESH_TOKEN));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new UserException(UserErrorStatus.EXPIRED_REFRESH_TOKEN);
        }

        User user = stored.getUser();

        // 새 토큰 발급 (issueRefreshToken 내부에서 deleteByUser로 기존 토큰 전부 삭제)
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId());
        String newRefreshToken = issueRefreshToken(user);

        return new TokenRefreshResponseDTO(newAccessToken, newRefreshToken);
    }

    // 로그아웃 - refresh token 무효화
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByToken(rawRefreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    // 이메일 존재 여부 확인
    @Transactional(readOnly = true)
    public EmailCheckResponseDTO checkEmailExists(String email) {
        boolean exists = userRepository.existsByEmail(email);
        return new EmailCheckResponseDTO(exists);
    }

    public void validateSignUpRequest(UserRequestDTO.SignUp request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException(UserErrorStatus.EXIST_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new UserException(UserErrorStatus.EXIST_NICKNAME);
        }
    }

    public School getSchoolByName(String schoolName) {
        if (ADMIN_SCHOOL_NAME.equals(schoolName)) {
            throw new SchoolException(SchoolErrorStatus.INVALID_SCHOOL_FOR_SIGNUP);
        }
        return schoolRepository.findByName(schoolName)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));
    }

    public User createUser(UserRequestDTO.SignUp request, School school, boolean phoneVerified) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .nickname(request.getNickname())
                .school(school)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .verified(false)
                .gender(request.getGender())
                .grade(request.getGrade())
                .phoneNumber(request.getPhoneNumber())
                .profileImageUrl(
                        request.getProfileImageUrl() != null ? request.getProfileImageUrl() : "default_profile.png"
                )
                .phoneVerified(phoneVerified)
                .build();
    }

    public void createVerificationRequest(User user, School school, String imageUrl) {
        UserSchoolVerificationRequest verificationRequest = new UserSchoolVerificationRequest(imageUrl, user, school);
        verificationRequestRepository.save(verificationRequest);
    }

    @Transactional(readOnly = true)
    public VerificationReapplyInfoResponseDTO getReapplyInfo(
            UserRequestDTO.VerificationReapplyInfoRequest request
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserException(UserErrorStatus.INVALID_PASSWORD);
        }

        if (user.isVerified()) {
            throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_ALREADY_APPROVED);
        }

        UserSchoolVerificationRequest latestRequest =
                verificationRequestRepository.findTopByUserOrderByRequestedAtDesc(user)
                        .orElseThrow(() -> new UserException(UserErrorStatus.SCHOOL_VERIFICATION_REQUIRED));

        switch (latestRequest.getStatus()) {
            case REJECTED -> {
                return new VerificationReapplyInfoResponseDTO(
                        latestRequest.getSchool().getId(),
                        latestRequest.getSchool().getName(),
                        latestRequest.getAdminComment()
                );
            }
            case PENDING -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_PENDING);
            case APPROVED -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_ALREADY_APPROVED);
            default -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_STATUS_INVALID);
        }
    }

    public Long reapplyVerification(
            UserRequestDTO.VerificationReapply request,
            String studentIdImageUrl
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserException(UserErrorStatus.INVALID_PASSWORD);
        }

        if (user.isVerified()) {
            throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_ALREADY_APPROVED);
        }

        boolean hasPending = verificationRequestRepository.existsByUserAndStatus(user, VerificationStatus.PENDING);
        if (hasPending) {
            throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_PENDING);
        }

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));

        UserSchoolVerificationRequest newRequest =
                UserSchoolVerificationRequest.builder()
                        .requestImageUrl(studentIdImageUrl)
                        .user(user)
                        .school(school)
                        .build();

        return verificationRequestRepository.save(newRequest).getId();
    }

    @Transactional(readOnly = true)
    public PhoneCheckResponseDTO checkPhoneNumberExists(String phoneNumber) {
        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
        boolean exists = userRepository.existsByPhoneNumber(normalizedPhoneNumber);
        return new PhoneCheckResponseDTO(exists);
    }

    @Transactional(readOnly = true)
    public NicknameCheckResponseDTO checkNicknameExists(String nickname) {
        boolean exists = userRepository.existsByNickname(nickname);
        return new NicknameCheckResponseDTO(exists);
    }

    // =================== private ===================

    private LoginResponseDTO buildLoginResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = issueRefreshToken(user);
        Long schoolId = user.getSchool() != null ? user.getSchool().getId() : null;
        return new LoginResponseDTO(user.getId(), accessToken, refreshToken, user.getRole().name(), schoolId);
    }

    // 새 refresh token 발급 (기존 것 대체)
    private String issueRefreshToken(User user) {
        // 기존 refresh token 모두 삭제 (1기기 1토큰 정책)
        refreshTokenRepository.deleteByUser(user);

        String tokenValue = UUID.randomUUID().toString();
        long expirationMs = jwtProperties.getRefreshTokenExpiration();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);

        refreshTokenRepository.save(new RefreshToken(tokenValue, user, expiresAt));
        return tokenValue;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("[^0-9]", "");
    }
}

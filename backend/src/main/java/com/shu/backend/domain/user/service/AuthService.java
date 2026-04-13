package com.shu.backend.domain.user.service;

import com.shu.backend.domain.auth.service.VerificationService;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.repository.SchoolRepository;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.user.dto.LoginResponseDTO;
import com.shu.backend.domain.user.dto.SignUpResponseDTO;
import com.shu.backend.domain.user.dto.UserLoginDTO;
import com.shu.backend.domain.user.dto.UserRequestDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.verification.entity.UserSchoolVerificationRequest;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRequestRepository;
import com.shu.backend.domain.verification.status.VerificationStatus;
import com.shu.backend.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입, 로그인, 학교 인증 재요청 등
 * 사용자 인증/가입 관련 비즈니스 로직을 처리하는 서비스.
 *
 * 사용자 생성, 비밀번호 검증, 학교 조회, 인증 요청 생성,
 * JWT 발급까지 인증 흐름 전반을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserSchoolVerificationRequestRepository verificationRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final VerificationService smsVerificationService;

    private static final String ADMIN_SCHOOL_NAME = "운영자전용학교";

    // 회원가입 처리
    public SignUpResponseDTO join(UserRequestDTO.SignUp request, String studentIdImageUrl) {

        // 이메일 / 닉네임 중복 검사
        validateSignUpRequest(request);

        // 이메일 인증 완료 여부 확인
        smsVerificationService.verifyTokenOrThrow(
                request.getVerificationToken(),
                request.getEmail()
        );

        // 학교 조회
        School school = getSchoolByName(request.getSchool());

        // 유저 생성 및 저장
        User newUser = createUser(request, school, true);
        userRepository.save(newUser);

        // 학교 인증 요청 생성
        createVerificationRequest(newUser, school, studentIdImageUrl);

        // JWT 발급
        String accessToken = jwtTokenProvider.createAccessToken(newUser.getId());

        return new SignUpResponseDTO(newUser.getId(), accessToken);
    }

    // 회원가입 시 중복 검사
    public void validateSignUpRequest(UserRequestDTO.SignUp request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException(UserErrorStatus.EXIST_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new UserException(UserErrorStatus.EXIST_NICKNAME);
        }
    }

    // 학교명으로 학교 조회
    public School getSchoolByName(String schoolName) {
        if (ADMIN_SCHOOL_NAME.equals(schoolName)) {
            throw new SchoolException(SchoolErrorStatus.INVALID_SCHOOL_FOR_SIGNUP);
        }

        return schoolRepository.findByName(schoolName)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));
    }

    // 회원가입용 User 엔티티 생성
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
                .classRoom(request.getClassRoom())
                .phoneNumber(request.getPhoneNumber())
                .profileImageUrl(
                        request.getProfileImageUrl() != null ? request.getProfileImageUrl() : "default_profile.png"
                )
                .phoneVerified(phoneVerified)
                .build();
    }

    // 학교 인증 요청 생성
    public void createVerificationRequest(User user, School school, String imageUrl) {

        UserSchoolVerificationRequest verificationRequest = new UserSchoolVerificationRequest(imageUrl,user,school);
        verificationRequestRepository.save(verificationRequest);
    }

    // 로그인 처리
    @Transactional(readOnly = true)
    public LoginResponseDTO login(UserLoginDTO userLoginDTO) {

        User user = userRepository.findByEmail(userLoginDTO.getEmail())
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));

        if (!passwordEncoder.matches(userLoginDTO.getPassword(), user.getPassword())) {
            throw new UserException(UserErrorStatus.INVALID_PASSWORD);
        }

        // 일반 사용자는 학교 인증 상태 확인
        if (user.getRole() == UserRole.USER) {

            // 이미 인증 완료된 경우 바로 로그인 허용
            if (user.isVerified()) {
                String accessToken = jwtTokenProvider.createAccessToken(user.getId());
                return new LoginResponseDTO(user.getId(), accessToken);
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

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        return new LoginResponseDTO(user.getId(), accessToken);
    }

    // 로그아웃 처리
    @Transactional(readOnly = true)
    public void logout(){
    }

    // 학교 인증 반려 후 재요청 처리
    public Long reapplyVerification(UserRequestDTO.VerificationReapply request) {

        // 유저 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserException(UserErrorStatus.INVALID_PASSWORD);
        }

        // 이미 인증 완료된 경우 재요청 불가
        if (user.isVerified()) {
            throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_ALREADY_APPROVED);
        }

        // 이미 대기 중인 요청이 있으면 재요청 불가
        boolean hasPending = verificationRequestRepository.existsByUserAndStatus(user, VerificationStatus.PENDING);
        if (hasPending) {
            throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_PENDING);
        }

        // 학교 조회
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));

        // 새 인증 요청 생성
        UserSchoolVerificationRequest newRequest =
                UserSchoolVerificationRequest.builder()
                        .requestImageUrl(request.getStudentIdImageUrl())
                        .user(user)
                        .school(school)
                        .build();

        return verificationRequestRepository.save(newRequest).getId();
    }

}
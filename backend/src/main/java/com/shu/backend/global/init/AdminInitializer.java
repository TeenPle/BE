package com.shu.backend.global.init;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.region.repository.RegionRepository;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.repository.SchoolRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.Gender;
import com.shu.backend.domain.user.enums.Grade;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private static final String ADMIN_SCHOOL_NAME = "운영자전용학교";
    private static final String TEST_SCHOOL_NAME = "오남고등학교";

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegionRepository regionRepository;
    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public void run(String... args) {

        System.out.println("java.version = " + System.getProperty("java.version"));
        System.out.println("java.home = " + System.getProperty("java.home"));
        System.out.println("javax.net.ssl.trustStore = " + System.getProperty("javax.net.ssl.trustStore"));

        // 지역 생성 or 조회
        Region adminRegion = regionRepository.findByName("의정부시")
                .orElseGet(() -> regionRepository.save(Region.builder().name("의정부시").build()));

        // 오남고등학교 생성 or 조회 (region 할당)
        School testSchool = schoolRepository.findByName(TEST_SCHOOL_NAME)
                .orElseGet(() -> schoolRepository.save(
                        School.builder()
                                .name(TEST_SCHOOL_NAME)
                                .region(adminRegion)
                                .logoImageUrl(null)
                                .build()
                ));
        if (testSchool.getRegion() == null) {
            testSchool.updateRegion(adminRegion);
        }

        // 운영자 전용 학교 생성 or 조회
        School adminSchool = schoolRepository.findByName(ADMIN_SCHOOL_NAME)
                .orElseGet(() -> schoolRepository.save(
                        School.builder()
                                .name(ADMIN_SCHOOL_NAME)
                                .region(adminRegion)
                                .logoImageUrl(null)
                                .build()
                ));

        // 오남고등학교 게시판 생성
        createBoardIfNotExists(testSchool, "자유게시판", "자유롭게 이야기해요", BoardScope.SCHOOL);
        createBoardIfNotExists(testSchool, "1학년 게시판", "1학년 전용 게시판", BoardScope.SCHOOL);
        createBoardIfNotExists(testSchool, "2학년 게시판", "2학년 전용 게시판", BoardScope.SCHOOL);
        createBoardIfNotExists(testSchool, "3학년 게시판", "3학년 전용 게시판", BoardScope.SCHOOL);
        createBoardIfNotExists(testSchool, "졸업생 게시판", "졸업생 전용 게시판", BoardScope.SCHOOL);
        createRegionBoardIfNotExists(adminRegion, "지역 게시판", "같은 지역 학생들과 이야기해요");

        // admin은 그대로 유지
        User admin = userRepository.findByEmail("leejd8131@naver.com")
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .username("시스템관리자1")
                                .email("leejd8131@naver.com")
                                .nickname("admin1")
                                .password(passwordEncoder.encode("dhkdrl12"))
                                .school(adminSchool)
                                .role(UserRole.ADMIN)
                                .status(UserStatus.ACTIVE)
                                .verified(true)
                                .profileImageUrl(null)
                                .grade(Grade.FIRST)
                                .phoneNumber("01053468130")
                                .gender(Gender.MALE)
                                .build()
                ));

        // admin2는 일반회원 + 오남고등학교 소속으로만 생성
        // 이미 같은 이메일 계정이 있으면 기존 계정을 그대로 사용
        User admin2 = userRepository.findByEmail("teenple@example.com")
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .username("카이사")
                                .email("teenple@example.com")
                                .nickname("카이사")
                                .password(passwordEncoder.encode("Abcd1234!"))
                                .school(testSchool)
                                .role(UserRole.USER)
                                .status(UserStatus.ACTIVE)
                                .verified(true)
                                .profileImageUrl(null)
                                .grade(Grade.FIRST)
                                .phoneNumber("01071651075")
                                .gender(Gender.MALE)
                                .phoneVerified(true)
                                .build()
                ));

        // 오남고 테스트 유저 2명
        createTestUserIfNotExists(
                "테스트유저1",
                "teenple1@example.com",
                "test1",
                "Abcd1234!",
                "01053468131",
                testSchool
        );

        createTestUserIfNotExists(
                "테스트유저2",
                "teenple2@example.com",
                "test2",
                "Abcd1234!",
                "01053468132",
                testSchool
        );

        // 새 오남고 테스트 학생 추가 - 닉네임 가렌
        createTestUserIfNotExists(
                "오남고테스트학생",
                "garen@example.com",
                "가렌",
                "Abcd1234!",
                "01053468133",
                testSchool
        );

        // 기존 시드 게시글/댓글을 전부 오남고등학교 소속으로 생성
        seedTestSchoolPosts(testSchool, admin2, admin);
    }

    /**
     * 학교 게시판이 없으면 생성하고, 있으면 기존 게시판 반환
     */
    private Board createBoardIfNotExists(
            School school,
            String title,
            String description,
            BoardScope scope
    ) {
        return boardRepository.findBySchoolAndTitle(school, title)
                .orElseGet(() -> boardRepository.save(
                        Board.builder()
                                .title(title)
                                .description(description)
                                .school(school)
                                .scope(scope)
                                .build()
                ));
    }

    /**
     * 지역 게시판이 없으면 생성하고, 있으면 기존 게시판 반환
     */
    private Board createRegionBoardIfNotExists(
            Region region,
            String title,
            String description
    ) {
        return boardRepository.findByRegionAndTitle(region, title)
                .orElseGet(() -> boardRepository.save(
                        Board.builder()
                                .title(title)
                                .description(description)
                                .region(region)
                                .scope(BoardScope.REGION)
                                .build()
                ));
    }

    /**
     * 테스트 유저가 없으면 생성하고, 있으면 기존 유저 반환
     */
    private User createTestUserIfNotExists(
            String username,
            String email,
            String nickname,
            String rawPassword,
            String phoneNumber,
            School school
    ) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = User.builder()
                            .username(username)
                            .email(email)
                            .nickname(nickname)
                            .password(passwordEncoder.encode(rawPassword))
                            .school(school)
                            .role(UserRole.USER)
                            .status(UserStatus.ACTIVE)
                            .verified(true)
                            .profileImageUrl(null)
                            .grade(Grade.FIRST)
                            .phoneNumber(phoneNumber)
                            .gender(Gender.MALE)
                            .phoneVerified(true)
                            .build();

                    return userRepository.save(user);
                });
    }

    /**
     * 오남고등학교 게시판별 게시글 / 댓글 시드 생성
     */
    private void seedTestSchoolPosts(
            School testSchool,
            User writer1,
            User writer2
    ) {
        Board freeBoard = boardRepository.findBySchoolAndTitle(testSchool, "자유게시판").orElseThrow();
        Board firstBoard = boardRepository.findBySchoolAndTitle(testSchool, "1학년 게시판").orElseThrow();
        Board secondBoard = boardRepository.findBySchoolAndTitle(testSchool, "2학년 게시판").orElseThrow();
        Board thirdBoard = boardRepository.findBySchoolAndTitle(testSchool, "3학년 게시판").orElseThrow();
        Board alumniBoard = boardRepository.findBySchoolAndTitle(testSchool, "졸업생 게시판").orElseThrow();

        seedBoardIfEmpty(freeBoard, writer1, writer2,
                List.of(
                        new SeedPost("기숙사 <<< 오지마라", "화장실 이게 맞나 진짜로", true, 17),
                        new SeedPost("나 기상.", "25일 스킵 성공\n아래 틴붕이는 얼른 헤어질 수 있도록 하렴", true, 82),
                        new SeedPost("오늘 급식 생각보다 괜찮았음", "국이 오랜만에 괜찮더라", true, 9),
                        new SeedPost("학교 와이파이 또 느림", "과제 제출할 때마다 이럼", false, 5)
                )
        );

        seedBoardIfEmpty(firstBoard, writer1, writer2,
                List.of(
                        new SeedPost("1학년 과학 수행 어때?", "자료 조사 어디까지 했냐", true, 11),
                        new SeedPost("1학년 체육대회 기대됨", "반티 뭐로 할지 고민 중", true, 6),
                        new SeedPost("수학 쌤 진도 너무 빠름", "오늘도 절반은 놓침", true, 13)
                )
        );

        seedBoardIfEmpty(secondBoard, writer1, writer2,
                List.of(
                        new SeedPost("2학년 모고 난이도 실화?", "국어 왜 이렇게 어려웠냐", true, 21),
                        new SeedPost("동아리 발표 준비 다 했냐", "PPT 아직 반밖에 못 함", false, 7),
                        new SeedPost("야자 자리 바꾸고 싶다", "에어컨 바람 직빵 자리임", true, 4)
                )
        );

        seedBoardIfEmpty(thirdBoard, writer1, writer2,
                List.of(
                        new SeedPost("수시 원서 준비 어디까지 했어?", "자소서 막판 수정 중", true, 28),
                        new SeedPost("3학년 야자 분위기 빡세다", "다들 예민해진 듯", true, 10),
                        new SeedPost("면접 준비 스터디 할 사람", "같이 연습할 사람 구함", false, 8)
                )
        );

        seedBoardIfEmpty(alumniBoard, writer1, writer2,
                List.of(
                        new SeedPost("졸업생 게시판 테스트 글", "졸업생 게시판도 잘 보이는지 확인", false, 3),
                        new SeedPost("대학 생활 질문 받아요", "후배들 궁금한 거 있으면 댓글 ㄱ", false, 14)
                )
        );
    }

    /**
     * 게시판에 글이 없을 때만 시드 데이터 생성
     */
    private void seedBoardIfEmpty(
            Board board,
            User writer1,
            User writer2,
            List<SeedPost> seedPosts
    ) {
        if (postRepository.countByBoard(board) > 0) {
            return;
        }

        for (int i = 0; i < seedPosts.size(); i++) {
            SeedPost seed = seedPosts.get(i);

            Post post = createPost(
                    board,
                    (i % 2 == 0) ? writer1 : writer2,
                    seed.title(),
                    seed.content(),
                    seed.anonymous(),
                    seed.likeCount()
            );

            Post savedPost = postRepository.save(post);

            Comment c1 = createComment(savedPost, writer2, "이 글 공감됨", true, null);
            createComment(savedPost, writer1, "나도 비슷하게 느낌", true, null);
            createComment(savedPost, writer1, "특히 오늘 더 그랬음", true, c1);
        }
    }

    /**
     * 게시글 생성
     */
    private Post createPost(
            Board board,
            User user,
            String title,
            String content,
            boolean anonymous,
            int likeCount
    ) {
        Post post = Post.builder()
                .board(board)
                .user(user)
                .title(title)
                .content(content)
                .anonymous(anonymous)
                .build();

        applyLikeCount(post, likeCount);

        return post;
    }

    /**
     * 댓글 생성
     */
    private Comment createComment(
            Post post,
            User user,
            String content,
            boolean anonymous,
            Comment parent
    ) {
        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(content)
                .anonymous(anonymous)
                .parent(parent)
                .build();

        return commentRepository.save(comment);
    }

    /**
     * Post 엔티티에 좋아요 수 반영
     */
    private void applyLikeCount(Post post, int likeCount) {
        // 필요 시 post 엔티티 메서드에 맞게 반영
    }

    /**
     * 게시글 시드용 간단 DTO
     */
    private record SeedPost(
            String title,
            String content,
            boolean anonymous,
            int likeCount
    ) {
    }
}
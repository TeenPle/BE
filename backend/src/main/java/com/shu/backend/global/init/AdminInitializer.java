package com.shu.backend.global.init;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.enums.BoardType;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.board.service.DefaultSchoolBoardService;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.reaction.entity.Reaction;
import com.shu.backend.domain.reaction.enums.ReactionTargetType;
import com.shu.backend.domain.reaction.repository.ReactionRepository;
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
import com.shu.backend.global.neis.NeisSchoolSyncProperties;
import com.shu.backend.global.neis.NeisSchoolSyncService;
import com.shu.backend.global.neis.NeisSyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
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
    private final ReactionRepository reactionRepository;
    private final DefaultSchoolBoardService defaultSchoolBoardService;
    private final NeisSchoolSyncService neisSchoolSyncService;
    private final NeisSchoolSyncProperties neisSchoolSyncProperties;

    @Override
    @Transactional
    public void run(String... args) {

        log.debug("AdminInitializer runtime: javaVersion={}, javaHome={}, trustStore={}",
                System.getProperty("java.version"),
                System.getProperty("java.home"),
                System.getProperty("javax.net.ssl.trustStore"));

//        importAllHighSchoolsWithDefaultBoards();

        // 지역 생성 or 조회
        Region adminRegion = regionRepository.findByName("의정부시")
                .orElseGet(() -> regionRepository.save(Region.builder().name("의정부시").build()));

        // 오남고등학교 생성 or 조회 (region 할당)
        School testSchool = schoolRepository.findFirstByNameOrderByIdAsc(TEST_SCHOOL_NAME)
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
        School adminSchool = schoolRepository.findFirstByNameOrderByIdAsc(ADMIN_SCHOOL_NAME)
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

        // admin은 그대로 유지
        defaultSchoolBoardService.ensureDefaultBoards(testSchool);

        userRepository.findByEmail("leejd8131@naver.com")
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

        User garen = createTestUserIfNotExists(
                "오남고테스트학생",
                "garen@example.com",
                "가렌",
                "Abcd1234!",
                "01053468133",
                testSchool
        );

        User lux = createTestUserIfNotExists(
                "오남고테스트학생2",
                "lux@example.com",
                "럭스",
                "Abcd1234!",
                "01053468134",
                testSchool
        );

        User caitlyn = createTestUserIfNotExists(
                "오남고테스트학생3",
                "caitlyn@example.com",
                "케이틀린",
                "Abcd1234!",
                "01053468135",
                testSchool
        );

        User sivir = createTestUserIfNotExists(
                "오남고테스트학생4",
                "sivir@example.com",
                "시비르",
                "Abcd1234!",
                "01053468136",
                testSchool
        );

        // 오남고 테스트 학생 5명이 균일하게 글/댓글/좋아요를 작성한 시드 데이터
        seedTestSchoolPosts(testSchool, List.of(admin2, garen, lux, caitlyn, sivir));
    }

//    private void importAllHighSchoolsWithDefaultBoards() {
//        if (!neisSchoolSyncProperties.isEnabled() || !neisSchoolSyncProperties.isRunOnStartup()) {
//            System.out.println("[NEIS School Import] AdminInitializer skipped: enabled="
//                    + neisSchoolSyncProperties.isEnabled()
//                    + ", runOnStartup="
//                    + neisSchoolSyncProperties.isRunOnStartup());
//            return;
//        }
//
//        NeisSyncResult result = neisSchoolSyncService.syncAllHighSchools(
//                false,
//                neisSchoolSyncProperties.isCreateBoards()
//        );
//        System.out.println("[NEIS School Import] AdminInitializer result = " + result);
//    }

    /**
     * 학교 게시판이 없으면 생성하고, 있으면 기존 게시판 반환
     */
    private Board createBoardIfNotExists(
            School school,
            String title,
            String description,
            BoardScope scope
    ) {
        if (scope == BoardScope.SCHOOL) {
            defaultSchoolBoardService.ensureDefaultBoards(school);
            return boardRepository.findBySchoolIdAndType(school.getId(), BoardType.FREE).orElse(null);
        }

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
    private void seedDefaultBoardPosts(School testSchool, List<User> testUsers) {
        Board freeBoard = boardRepository.findBySchoolIdAndType(testSchool.getId(), BoardType.FREE).orElseThrow();
        Board firstBoard = boardRepository.findBySchoolIdAndType(testSchool.getId(), BoardType.GRADE_1).orElseThrow();
        Board secondBoard = boardRepository.findBySchoolIdAndType(testSchool.getId(), BoardType.GRADE_2).orElseThrow();
        Board thirdBoard = boardRepository.findBySchoolIdAndType(testSchool.getId(), BoardType.GRADE_3).orElseThrow();
        Board alumniBoard = boardRepository.findBySchoolIdAndType(testSchool.getId(), BoardType.GRADUATE).orElseThrow();

        seedBoardIfNeeded(freeBoard, "자유", testUsers, 0);
        seedBoardIfNeeded(firstBoard, "1학년", testUsers, 1);
        seedBoardIfNeeded(secondBoard, "2학년", testUsers, 2);
        seedBoardIfNeeded(thirdBoard, "3학년", testUsers, 3);
        seedBoardIfNeeded(alumniBoard, "졸업생", testUsers, 4);
    }

    private void seedTestSchoolPosts(School testSchool, List<User> testUsers) {
        if (boardRepository.findBySchoolIdAndType(testSchool.getId(), BoardType.FREE).isPresent()) {
            seedDefaultBoardPosts(testSchool, testUsers);
            return;
        }
        Board freeBoard = boardRepository.findBySchoolAndTitle(testSchool, "자유게시판").orElseThrow();
        Board firstBoard = boardRepository.findBySchoolAndTitle(testSchool, "1학년 게시판").orElseThrow();
        Board secondBoard = boardRepository.findBySchoolAndTitle(testSchool, "2학년 게시판").orElseThrow();
        Board thirdBoard = boardRepository.findBySchoolAndTitle(testSchool, "3학년 게시판").orElseThrow();
        Board alumniBoard = boardRepository.findBySchoolAndTitle(testSchool, "졸업생 게시판").orElseThrow();

        seedBoardIfNeeded(freeBoard, "자유", testUsers, 0);
        seedBoardIfNeeded(firstBoard, "1학년", testUsers, 1);
        seedBoardIfNeeded(secondBoard, "2학년", testUsers, 2);
        seedBoardIfNeeded(thirdBoard, "3학년", testUsers, 3);
        seedBoardIfNeeded(alumniBoard, "졸업생", testUsers, 4);
    }

    /**
     * 게시판별 10개 글을 생성하되, 같은 제목의 시드 글은 중복 생성하지 않는다.
     */
    private void seedBoardIfNeeded(
            Board board,
            String boardLabel,
            List<User> testUsers,
            int boardOffset
    ) {
        List<SeedPost> seedPosts = createSeedPosts(boardLabel);

        for (int i = 0; i < seedPosts.size(); i++) {
            SeedPost seed = seedPosts.get(i);
            if (postRepository.existsByBoardAndTitle(board, seed.title())) {
                continue;
            }

            User writer = testUsers.get((boardOffset * 2 + i) % testUsers.size());

            Post post = createPost(
                    board,
                    writer,
                    seed.title(),
                    seed.content(),
                    seed.anonymous(),
                    seed.likeCount()
            );

            Post savedPost = postRepository.save(post);
            seedComments(savedPost, testUsers, writer, seed.commentCount(), i);
            seedPostLikes(savedPost, testUsers, writer, seed.likeCount(), i);
        }
    }

    private List<SeedPost> createSeedPosts(String boardLabel) {
        return List.of(
                new SeedPost(boardLabel + " 공지 확인했어?", boardLabel + " 게시판 공지 본 사람 댓글 남겨줘.", false, 0, 0),
                new SeedPost(boardLabel + " 오늘 급식 후기", "생각보다 괜찮았는지 다들 의견 궁금함.", true, 1, 2),
                new SeedPost(boardLabel + " 수행평가 일정 공유", "헷갈리는 일정 있으면 여기서 같이 정리하자.", false, 3, 5),
                new SeedPost(boardLabel + " 야자 자리 질문", "자리 바꾸고 싶은 사람 있는지 확인 중.", true, 5, 1),
                new SeedPost(boardLabel + " 동아리 모집 봤어?", "이번 모집 공지 중에 괜찮은 동아리 추천해줘.", false, 8, 4),
                new SeedPost(boardLabel + " 등교길 버스 상황", "아침에 버스 많이 밀렸는지 공유 부탁.", true, 13, 7),
                new SeedPost(boardLabel + " 시험 범위 체크", "프린트 포함인지 아닌지 헷갈려서 확인해보자.", false, 21, 10),
                new SeedPost(boardLabel + " 체육복 챙겨야 함?", "내일 수업 준비물 아는 사람?", true, 34, 3),
                new SeedPost(boardLabel + " 매점 추천 메뉴", "요즘 제일 괜찮은 메뉴 뭐야?", true, 55, 14),
                new SeedPost(boardLabel + " 주말 스터디 구함", "같이 공부할 사람 있으면 댓글 줘.", false, 89, 8)
        );
    }

    private void seedComments(Post post, List<User> testUsers, User writer, int commentCount, int postIndex) {
        List<User> commenters = testUsers.stream()
                .filter(user -> !user.getId().equals(writer.getId()))
                .toList();

        Comment firstComment = null;

        for (int i = 0; i < commentCount; i++) {
            User commenter = commenters.get((postIndex + i) % commenters.size());
            Comment parent = i > 0 && i % 4 == 0 ? firstComment : null;
            Comment comment = createComment(
                    post,
                    commenter,
                    seedCommentContent(i),
                    i % 3 != 1,
                    parent
            );

            if (firstComment == null) {
                firstComment = comment;
            }
        }
    }

    private String seedCommentContent(int index) {
        List<String> comments = List.of(
                "나도 이거 궁금했어.",
                "확인해보니까 맞는 것 같아.",
                "정보 공유 고마워.",
                "이건 선생님께 다시 물어봐야 할 듯.",
                "우리 반도 비슷하게 이야기 나왔어.",
                "내일 아침에 다시 확인해볼게.",
                "생각보다 많은 사람이 헷갈리는 듯.",
                "이거 지난번에도 얘기 나왔었어.",
                "나는 반대로 들었는데 확인 필요함.",
                "정리되면 본문에도 추가해줘."
        );
        return comments.get(index % comments.size());
    }

    private void seedPostLikes(Post post, List<User> testUsers, User writer, int likeCount, int postIndex) {
        int reactionCount = Math.min(likeCount, testUsers.size() - 1);
        int cursor = postIndex + 1;
        int created = 0;

        while (created < reactionCount) {
            User voter = testUsers.get(cursor % testUsers.size());
            cursor++;

            if (voter.getId().equals(writer.getId())) {
                continue;
            }

            Reaction reaction = Reaction.create(ReactionTargetType.POST, post.getId(), voter.getId());
            reaction.applyLike();
            reactionRepository.save(reaction);
            created++;
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
        return Post.builder()
                .board(board)
                .user(user)
                .title(title)
                .content(content)
                .anonymous(anonymous)
                .likeCount(likeCount)
                .build();
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

        Comment saved = commentRepository.save(comment);
        post.incrementCommentCount();
        postRepository.save(post);
        return saved;
    }

    /**
     * 게시글 시드용 간단 DTO
     */
    private record SeedPost(
            String title,
            String content,
            boolean anonymous,
            int likeCount,
            int commentCount
    ) {
    }
}

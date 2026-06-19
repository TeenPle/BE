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
    private static final List<OperationalPost> OPERATIONAL_POSTS = List.of(
            new OperationalPost(
                    "틴플 이용 안내",
                    """
                    틴플은 학교 인증을 완료한 학생들이 함께 소통하는 학교 기반 커뮤니티입니다.

                    서로를 존중하며 자유롭게 이야기해 주세요. 학교폭력, 괴롭힘, 혐오·차별 표현, 허위 사실 유포, 성적 수치심을 유발하는 내용, 불법·광고성 게시물, 도배, 타인의 개인정보 공개는 제한될 수 있습니다.

                    자세한 이용약관과 개인정보처리방침은 앱 설정에서 확인할 수 있습니다.
                    """
            ),
            new OperationalPost(
                    "우리 학교 게시판 첫 글을 남겨보세요",
                    """
                    아직 조용한 게시판이라면 여러분이 첫 이야기를 시작해 주세요.

                    오늘 급식 후기, 시험·수행평가 정보, 동아리와 행사 소식, 등굣길 정보, 학교생활 질문처럼 친구들과 나누고 싶은 내용을 자유롭게 작성할 수 있습니다.

                    개인을 특정하거나 상처를 줄 수 있는 내용은 피하고, 서로에게 도움이 되는 학교 커뮤니티를 함께 만들어 주세요.
                    """
            ),
            new OperationalPost(
                    "불편하거나 위험한 게시글 신고 방법",
                    """
                    불편하거나 위험한 게시글과 댓글을 발견하면 해당 콘텐츠의 오른쪽 위 메뉴에서 '신고하기'를 선택해 주세요.

                    스팸, 욕설·모욕, 음란물·선정적 내용, 불법 콘텐츠, 괴롭힘 등의 사유로 신고할 수 있습니다. 접수된 신고는 운영자가 확인하고 필요한 경우 게시물 숨김, 경고 또는 이용 제한 조치를 진행합니다.

                    신고 기능은 안전한 커뮤니티를 위한 기능입니다. 허위 신고나 반복적인 신고 기능 악용은 삼가 주세요.
                    """
            )
    );

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

        /*
         * 실행 환경 전환 지점
         *
         * 로컬 개발: initializeLocalData() 호출을 열고 initializeProductionData()를 주석 처리한다.
         * 실제 배포: initializeLocalData()를 주석 처리하고 initializeProductionData() 호출을 연다.
         */
        initializeLocalData();
//        initializeProductionData();
    }

    /**
     * 로컬 개발용 초기 데이터.
     *
     * 현재 사용 중인 학교, 계정, 게시글/댓글/좋아요 테스트 데이터를 그대로 구성한다.
     */
    private void initializeLocalData() {
//        importAllHighSchoolsWithDefaultBoards();

        // 지역 생성 or 조회
        Region adminRegion = getOrCreateAdminRegion();

        // 오남고등학교 생성 or 조회 (region 할당)
        School testSchool = getOrCreateTestSchool(adminRegion);

        // 운영자 전용 학교 생성 or 조회
        School adminSchool = getOrCreateAdminSchool(adminRegion);

        // 오남고등학교 게시판 생성
        ensureTestSchoolBoards(testSchool);

        createPrimaryAdminIfNotExists(adminSchool);

        User testAccount = createAppTestAccountIfNotExists(testSchool);

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
        seedTestSchoolPosts(testSchool, List.of(testAccount, garen, lux, caitlyn, sivir));
    }

    /**
     * 실제 배포용 초기 데이터.
     *
     * 관리자 계정 2개와 앱 검수용 테스트 계정 1개를 구성한다.
     * 게시글/댓글/좋아요 테스트 데이터 정리는 이번 변경 범위에 포함하지 않는다.
     */
    private void initializeProductionData() {
        importAllHighSchoolsWithDefaultBoards();

        Region adminRegion = getOrCreateAdminRegion();
        School adminSchool = getOrCreateAdminSchool(adminRegion);
        School testSchool = getOrCreateTestSchool(adminRegion);

        User operationalAdmin = createPrimaryAdminIfNotExists(adminSchool);
        createSecondaryAdminIfNotExists(adminSchool);
        ensureTestSchoolBoards(testSchool);
        createAppTestAccountIfNotExists(testSchool);
        seedOperationalPostsForAllSchools(operationalAdmin);
    }

    private Region getOrCreateAdminRegion() {
        return regionRepository.findByName("의정부시")
                .orElseGet(() -> regionRepository.save(
                        Region.builder()
                                .name("의정부시")
                                .build()
                ));
    }

    private School getOrCreateTestSchool(Region region) {
        School testSchool = schoolRepository.findFirstByNameOrderByIdAsc(TEST_SCHOOL_NAME)
                .orElseGet(() -> schoolRepository.save(
                        School.builder()
                                .name(TEST_SCHOOL_NAME)
                                .region(region)
                                .logoImageUrl(null)
                                .build()
                ));

        if (testSchool.getRegion() == null) {
            testSchool.updateRegion(region);
        }
        return testSchool;
    }

    private School getOrCreateAdminSchool(Region region) {
        return schoolRepository.findFirstByNameOrderByIdAsc(ADMIN_SCHOOL_NAME)
                .orElseGet(() -> schoolRepository.save(
                        School.builder()
                                .name(ADMIN_SCHOOL_NAME)
                                .region(region)
                                .logoImageUrl(null)
                                .build()
                ));
    }

    private void ensureTestSchoolBoards(School testSchool) {
        createBoardIfNotExists(testSchool, "자유게시판", "자유롭게 이야기해요", BoardScope.SCHOOL);
        createBoardIfNotExists(testSchool, "1학년 게시판", "1학년 전용 게시판", BoardScope.SCHOOL);
        createBoardIfNotExists(testSchool, "2학년 게시판", "2학년 전용 게시판", BoardScope.SCHOOL);
        createBoardIfNotExists(testSchool, "3학년 게시판", "3학년 전용 게시판", BoardScope.SCHOOL);
        createBoardIfNotExists(testSchool, "졸업생 게시판", "졸업생 전용 게시판", BoardScope.SCHOOL);
        defaultSchoolBoardService.ensureDefaultBoards(testSchool);
    }

    private User createPrimaryAdminIfNotExists(School adminSchool) {
        return userRepository.findByEmail("leejd8131@naver.com")
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .username("시스템관리자1")
                                .email("leejd8131@naver.com")
                                .nickname("admin1")
                                .password(passwordEncoder.encode("@a13688631"))
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
    }

    private User createSecondaryAdminIfNotExists(School adminSchool) {
        return userRepository.findByEmail("rkdgusals@naver.com")
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .username("시스템관리자2")
                                .email("rkdgusals@naver.com")
                                .nickname("admin2")
                                .password(passwordEncoder.encode("1234"))
                                .school(adminSchool)
                                .role(UserRole.ADMIN)
                                .status(UserStatus.ACTIVE)
                                .verified(true)
                                .profileImageUrl(null)
                                .grade(Grade.FIRST)
                                .phoneNumber("01053468131")
                                .gender(Gender.MALE)
                                .phoneVerified(true)
                                .build()
                ));
    }

    private User createAppTestAccountIfNotExists(School testSchool) {
        return userRepository.findByEmail("teenple@example.com")
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
    }

    /**
     * 운영자 전용 학교를 제외한 모든 학교의 자유게시판에 운영 안내 글을 생성한다.
     *
     * 같은 제목의 글이 이미 있으면 다시 만들지 않으므로 배포 초기화를 재실행해도 중복되지 않는다.
     */
    private void seedOperationalPostsForAllSchools(User operationalAdmin) {
        List<School> schools = schoolRepository.findAll().stream()
                .filter(school -> !ADMIN_SCHOOL_NAME.equals(school.getName()))
                .toList();

        int createdCount = 0;
        for (School school : schools) {
            defaultSchoolBoardService.ensureDefaultBoards(school);
            Board freeBoard = boardRepository.findBySchoolIdAndType(school.getId(), BoardType.FREE)
                    .orElseThrow(() -> new IllegalStateException(
                            "자유게시판 초기화 실패: schoolId=" + school.getId()
                    ));

            for (OperationalPost operationalPost : OPERATIONAL_POSTS) {
                if (postRepository.existsByBoardAndTitle(freeBoard, operationalPost.title())) {
                    continue;
                }

                postRepository.save(
                        createPost(
                                freeBoard,
                                operationalAdmin,
                                operationalPost.title(),
                                operationalPost.content(),
                                false,
                                0
                        )
                );
                createdCount++;
            }
        }

        log.info("[Operational Post Seed] completed: schools={}, created={}",
                schools.size(), createdCount);
    }

    private void importAllHighSchoolsWithDefaultBoards() {
        if (!neisSchoolSyncProperties.isEnabled() || !neisSchoolSyncProperties.isRunOnStartup()) {
            log.info("[NEIS School Import] skipped: enabled={}, runOnStartup={}",
                    neisSchoolSyncProperties.isEnabled(),
                    neisSchoolSyncProperties.isRunOnStartup());
            return;
        }

        NeisSyncResult result = neisSchoolSyncService.syncAllHighSchools(
                false,
                neisSchoolSyncProperties.isCreateBoards()
        );
        log.info("[NEIS School Import] result={}", result);
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

    private record OperationalPost(
            String title,
            String content
    ) {
    }
}

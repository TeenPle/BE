package com.shu.backend.global.moderation;

import com.shu.backend.domain.comment.exception.CommentException;
import com.shu.backend.domain.comment.exception.status.CommentErrorStatus;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 미성년자 대상 서비스의 UGC(게시글·댓글) 텍스트 자동 필터링 서비스.
 *
 * 기준: "맥락과 무관하게 항상 유해한 표현"만 즉시 차단한다.
 * 욕설·비방 등 맥락에 따라 달라지는 표현은 신고 시스템으로 위임한다.
 *
 * 위반 카테고리별로 구체적인 안내 메시지를 반환한다.
 */
@Service
public class ContentModerationService {

    // ─── 위반 카테고리 ────────────────────────────────────────────────────────────
    public enum ViolationType {
        SELF_HARM, SEXUAL, DRUG, CONTACT
    }

    // ─── 자해·자살 구체적 방법 유도 ─────────────────────────────────────────────
    private static final List<String> SELF_HARM_KEYWORDS = List.of(
            "자살방법", "자살하는법", "자살하는방법",
            "손목긋는법", "손목긋는방법",
            "목매는법", "목매다는법", "목을매는법",
            "투신하는법",
            "수면제먹는법", "수면제몇알",
            "농약먹으면", "농약마시면",
            "동반자살", "동반자해"
    );

    // ─── 성매매·불법 성적 거래 유도 ─────────────────────────────────────────────
    private static final List<String> SEXUAL_KEYWORDS = List.of(
            "조건만남", "조건녀", "조건알바",
            "원조교제", "원조",
            "성매매", "성매수",
            "몸파는", "몸팝니다", "몸팔아",
            "야동판매", "야동삽니다", "야설판매",
            "섹파구함", "섹파모집",
            "보지", "자지", "보빨", "딸딸이"
    );

    // ─── 불법 약물 거래 유도 ────────────────────────────────────────────────────
    private static final List<String> DRUG_KEYWORDS = List.of(
            "필로폰", "히로뽕",
            "마약팔아", "마약삽니다", "마약구해",
            "대마팔아", "대마삽니다", "대마구해",
            "약팔아요", "약구합니다", "떨팔아", "떨구해"
    );

    // ─── 외부 개인연락 유도 패턴 ─────────────────────────────────────────────────
    private static final List<Pattern> CONTACT_PATTERNS = List.of(
            Pattern.compile("01[016789][\\s\\-]?\\d{3,4}[\\s\\-]?\\d{4}"),
            Pattern.compile("오픈채팅|카카오톡\\s*(아이디|id|ID)|카톡\\s*(아이디|id|ID)"),
            Pattern.compile("텔레그램.{0,10}(아이디|id|@|ID)|라인.{0,10}(아이디|id|ID)")
    );

    private final Set<String> selfHarmSet;
    private final Set<String> sexualSet;
    private final Set<String> drugSet;

    public ContentModerationService() {
        this.selfHarmSet = toNormalizedSet(SELF_HARM_KEYWORDS);
        this.sexualSet   = toNormalizedSet(SEXUAL_KEYWORDS);
        this.drugSet     = toNormalizedSet(DRUG_KEYWORDS);
    }

    // ─── 공개 API ────────────────────────────────────────────────────────────────

    /** 게시글 제목·내용 검사. 위반 카테고리에 맞는 PostException 발생. */
    public void checkPost(String title, String content) {
        ViolationType violation = detect(title);
        if (violation == null) violation = detect(content);
        if (violation != null) throw new PostException(toPostStatus(violation));
    }

    /** 댓글 내용 검사. 위반 카테고리에 맞는 CommentException 발생. */
    public void checkComment(String content) {
        ViolationType violation = detect(content);
        if (violation != null) throw new CommentException(toCommentStatus(violation));
    }

    // ─── 내부 탐지 로직 ──────────────────────────────────────────────────────────

    private ViolationType detect(String text) {
        if (text == null || text.isBlank()) return null;

        String normalized = normalize(text);

        if (containsAny(normalized, selfHarmSet)) return ViolationType.SELF_HARM;
        if (containsAny(normalized, sexualSet))   return ViolationType.SEXUAL;
        if (containsAny(normalized, drugSet))     return ViolationType.DRUG;
        if (matchesContact(text))                 return ViolationType.CONTACT;

        return null;
    }

    private boolean containsAny(String normalized, Set<String> keywords) {
        for (String keyword : keywords) {
            if (normalized.contains(keyword)) return true;
        }
        return false;
    }

    private boolean matchesContact(String text) {
        for (Pattern pattern : CONTACT_PATTERNS) {
            if (pattern.matcher(text).find()) return true;
        }
        return false;
    }

    // ─── 카테고리 → ErrorStatus 매핑 ─────────────────────────────────────────────

    private PostErrorStatus toPostStatus(ViolationType type) {
        return switch (type) {
            case SELF_HARM -> PostErrorStatus.SELF_HARM_CONTENT;
            case SEXUAL    -> PostErrorStatus.SEXUAL_CONTENT;
            case DRUG      -> PostErrorStatus.DRUG_CONTENT;
            case CONTACT   -> PostErrorStatus.CONTACT_SOLICITATION;
        };
    }

    private CommentErrorStatus toCommentStatus(ViolationType type) {
        return switch (type) {
            case SELF_HARM -> CommentErrorStatus.SELF_HARM_CONTENT;
            case SEXUAL    -> CommentErrorStatus.SEXUAL_CONTENT;
            case DRUG      -> CommentErrorStatus.DRUG_CONTENT;
            case CONTACT   -> CommentErrorStatus.CONTACT_SOLICITATION;
        };
    }

    // ─── 유틸 ────────────────────────────────────────────────────────────────────

    private Set<String> toNormalizedSet(List<String> keywords) {
        return keywords.stream()
                .map(this::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 공백·특수문자 제거 후 소문자 변환. "자 살", "자@살" 같은 우회 시도 방어. */
    private String normalize(String text) {
        return text.replaceAll("[\\s\\p{Punct}]", "").toLowerCase();
    }
}

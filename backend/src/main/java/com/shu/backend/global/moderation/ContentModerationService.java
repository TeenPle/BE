package com.shu.backend.global.moderation;

import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.comment.exception.CommentException;
import com.shu.backend.domain.comment.exception.status.CommentErrorStatus;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private static final List<Pattern> CONTACT_PATTERNS = List.of(
            Pattern.compile("01[016789][\\s\\-]?\\d{3,4}[\\s\\-]?\\d{4}"),
            Pattern.compile("(?i)(kakao|카카오|카톡|오픈채팅|telegram|텔레그램|line|라인).{0,12}(id|아이디|@)"),
            Pattern.compile("(?i)(instagram|인스타|discord|디스코드).{0,12}(id|아이디|@)")
    );

    private final TextNormalizationService normalizationService;
    private final ContentFilterDictionaryService dictionaryService;

    public void checkPost(String title, String content) {
        if (isBlocked(title) || isBlocked(content)) {
            throw new PostException(PostErrorStatus.INAPPROPRIATE_CONTENT);
        }
        if (matchesContact(title) || matchesContact(content)) {
            throw new PostException(PostErrorStatus.CONTACT_SOLICITATION);
        }
    }

    public void checkComment(String content) {
        if (isBlocked(content)) {
            throw new CommentException(CommentErrorStatus.INAPPROPRIATE_CONTENT);
        }
        if (matchesContact(content)) {
            throw new CommentException(CommentErrorStatus.CONTACT_SOLICITATION);
        }
    }

    public void checkChatMessage(String content) {
        if (isBlocked(content) || matchesContact(content)) {
            throw new ChatMessageException(ChatMessageErrorStatus.INAPPROPRIATE_TEXT_CONTENT);
        }
    }

    private boolean isBlocked(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = normalizationService.normalize(text);
        ContentFilterMatch match = dictionaryService.findBlockedTerm(normalized);
        return match != null;
    }

    private boolean matchesContact(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (Pattern pattern : CONTACT_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}

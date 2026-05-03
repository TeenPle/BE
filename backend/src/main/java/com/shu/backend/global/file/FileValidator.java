package com.shu.backend.global.file;

import com.shu.backend.domain.media.exception.MediaException;
import com.shu.backend.domain.media.exception.status.MediaErrorStatus;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

/**
 * 파일 업로드 보안 검증 유틸리티.
 *
 * 검증 항목:
 *  1. MIME 타입 화이트리스트
 *  2. 매직바이트 (실제 파일 내용이 선언된 형식과 일치하는지)
 *  3. 파일명 정제 (경로 순회 방지)
 */
public final class FileValidator {

    private FileValidator() {}

    // ─── 허용 MIME 타입 ───────────────────────────────────────────────────────────
    private static final Set<String> IMAGE_ONLY_TYPES = Set.of(
            "image/jpeg", "image/png"
    );

    private static final Set<String> POST_MEDIA_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/quicktime"
    );

    // ─── 공개 API ────────────────────────────────────────────────────────────────

    /**
     * 학생증 이미지 검증 (jpg, png만 허용).
     * 위반 시 UserException 발생.
     */
    public static byte[] validateStudentCard(MultipartFile file) {
        try {
            validateMimeType(file.getContentType(), IMAGE_ONLY_TYPES, "학생증");
            byte[] bytes = readBytes(file);
            validateMagicBytes(bytes, file.getContentType(), "학생증");
            return bytes;
        } catch (UserException e) {
            throw e;
        } catch (IOException e) {
            throw new UserException(UserErrorStatus.USER_STUDENT_CARD_UPLOAD_FAIL);
        }
    }

    /**
     * 게시글 미디어 검증 (jpg, png, gif, webp, mp4, mov 허용).
     * 위반 시 MediaException 발생.
     */
    public static byte[] validatePostMedia(MultipartFile file) {
        try {
            if (!POST_MEDIA_TYPES.contains(file.getContentType())) {
                throw new MediaException(MediaErrorStatus.INVALID_FILE_TYPE);
            }
            byte[] bytes = readBytes(file);
            validateMagicBytesForMedia(bytes, file.getContentType());
            return bytes;
        } catch (MediaException e) {
            throw e;
        } catch (IOException e) {
            throw new MediaException(MediaErrorStatus.POST_MEDIA_UPLOAD_FAIL);
        }
    }

    /**
     * 파일명에서 경로 순회 문자를 제거하고 안전한 파일명을 반환한다.
     * 원본 파일명 대신 반드시 이 메서드를 거쳐야 한다.
     */
    public static String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return "unknown";
        // 경로 구분자, 상위 디렉토리 참조 제거
        String name = originalFilename
                .replaceAll("[\\\\/]", "")   // \ / 제거
                .replaceAll("\\.\\.", "")      // .. 제거
                .replaceAll("[^a-zA-Z0-9._\\-가-힣]", "_"); // 허용 문자 외 치환
        return name.isBlank() ? "unknown" : name;
    }

    /**
     * 확장자 추출 (sanitize된 파일명에서만 호출).
     */
    public static String extractSafeExt(String contentType) {
        return switch (contentType) {
            case "image/jpeg"       -> ".jpg";
            case "image/png"        -> ".png";
            case "image/gif"        -> ".gif";
            case "image/webp"       -> ".webp";
            case "video/mp4"        -> ".mp4";
            case "video/quicktime"  -> ".mov";
            default -> "";
        };
    }

    // ─── 내부 검증 로직 ──────────────────────────────────────────────────────────

    private static void validateMimeType(String contentType, Set<String> allowed, String context) {
        if (contentType == null || !allowed.contains(contentType)) {
            throw new UserException(UserErrorStatus.INVALID_FILE_TYPE);
        }
    }

    private static void validateMagicBytes(byte[] bytes, String contentType, String context) {
        if (!matchesMagic(bytes, contentType)) {
            throw new UserException(UserErrorStatus.INVALID_FILE_FORMAT);
        }
    }

    private static void validateMagicBytesForMedia(byte[] bytes, String contentType) {
        if (!matchesMagic(bytes, contentType)) {
            throw new MediaException(MediaErrorStatus.INVALID_FILE_FORMAT);
        }
    }

    /**
     * 매직바이트로 실제 파일 포맷 검증.
     *
     * JPEG : FF D8 FF
     * PNG  : 89 50 4E 47 0D 0A 1A 0A
     * GIF  : 47 49 46 38 (GIF8)
     * WebP : 52 49 46 46 ?? ?? ?? ?? 57 45 42 50 (RIFF....WEBP)
     * MP4  : offset 4에 66 74 79 70 (ftyp)
     * MOV  : offset 4에 66 74 79 70 (ftyp) 또는 6D 6F 6F 76 (moov)
     */
    private static boolean matchesMagic(byte[] b, String contentType) {
        if (b == null || b.length < 12) return false;

        return switch (contentType) {
            case "image/jpeg" ->
                    b[0] == (byte) 0xFF && b[1] == (byte) 0xD8 && b[2] == (byte) 0xFF;

            case "image/png" ->
                    b[0] == (byte) 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                    && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A;

            case "image/gif" ->
                    b[0] == 0x47 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x38;

            case "image/webp" ->
                    b[0] == 0x52 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x46
                    && b[8] == 0x57 && b[9] == 0x45 && b[10] == 0x42 && b[11] == 0x50;

            case "video/mp4", "video/quicktime" ->
                    b.length >= 8
                    && b[4] == 0x66 && b[5] == 0x74 && b[6] == 0x79 && b[7] == 0x70;

            default -> false;
        };
    }

    private static byte[] readBytes(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        if (bytes.length == 0) throw new IOException("빈 파일");
        return bytes;
    }
}

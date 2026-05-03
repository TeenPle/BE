package com.shu.backend.global.file;

import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.media.exception.MediaException;
import com.shu.backend.domain.media.exception.status.MediaErrorStatus;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileStorageProperties props;

    // 학생증 presigned URL 만료 시간 (15분)
    private static final Duration STUDENT_CARD_PRESIGNED_EXPIRY = Duration.ofMinutes(15);

    // 학생증 이미지 업로드 → 프라이빗 버킷에 저장, S3 key 반환
    @Override
    public String uploadStudentCardImage(MultipartFile file) {
        try {
            return uploadToStudentCardBucket(file);
        } catch (Exception e) {
            log.error("학생증 업로드 실패", e);
            throw new UserException(UserErrorStatus.USER_STUDENT_CARD_UPLOAD_FAIL);
        }
    }

    // 채팅 이미지 업로드
    @Override
    public String uploadChatImage(MultipartFile file) {
        try {
            return upload(file, props.getChatDir());
        } catch (Exception e) {
            log.error("채팅 이미지 업로드 실패", e);
            throw new ChatMessageException(ChatMessageErrorStatus.CHAT_IMAGE_UPLOAD_FAIL);
        }
    }

    // 게시글 미디어 업로드
    @Override
    public String uploadPostMedia(MultipartFile file) {
        try {
            return upload(file, props.getPostDir());
        } catch (Exception e) {
            log.error("게시글 미디어 업로드 실패", e);
            throw new MediaException(MediaErrorStatus.POST_MEDIA_UPLOAD_FAIL);
        }
    }

    // 프로필 이미지 업로드
    @Override
    public String uploadProfileImage(MultipartFile file) {
        try {
            return upload(file, "profile");
        } catch (Exception e) {
            log.error("프로필 이미지 업로드 실패", e);
            throw new UserException(UserErrorStatus.USER_STUDENT_CARD_UPLOAD_FAIL);
        }
    }

    // 학생증 presigned URL 발급 (15분 유효)
    @Override
    public String generateStudentCardPresignedUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(STUDENT_CARD_PRESIGNED_EXPIRY)
                .getObjectRequest(
                        GetObjectRequest.builder()
                                .bucket(props.getStudentCardBucket())
                                .key(key)
                                .build()
                )
                .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();
        log.info("학생증 presigned URL 발급: key={}", key);
        return url;
    }

    // 퍼블릭 버킷 파일 삭제 (프로필 이미지, 게시글 미디어 등)
    @Override
    public void deletePublicFile(String url) {
        if (url == null || url.isBlank()) return;
        String key = extractKeyFromUrl(url);
        if (key == null) {
            log.warn("퍼블릭 파일 삭제 생략 — key 추출 실패: url={}", url);
            return;
        }
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(props.getBucket())
                            .key(key)
                            .build()
            );
            log.info("퍼블릭 S3 파일 삭제 완료: key={}", key);
        } catch (Exception e) {
            log.warn("퍼블릭 S3 파일 삭제 실패 (무시): key={}, err={}", key, e.getMessage());
        }
    }

    // 학생증 이미지 삭제 (프라이빗 버킷)
    @Override
    public void deleteStudentCardImage(String key) {
        if (key == null || key.isBlank()) {
            log.warn("학생증 삭제 생략 — key가 비어있음");
            return;
        }
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(props.getStudentCardBucket())
                        .key(key)
                        .build()
        );
        log.info("학생증 S3 삭제 완료: key={}", key);
    }

    // 학생증 전용 업로드 (프라이빗 버킷, key만 반환)
    private String uploadToStudentCardBucket(MultipartFile file) throws IOException {
        // MIME 타입·매직바이트 검증 (jpg, png만 허용)
        byte[] bytes = FileValidator.validateStudentCard(file);

        String ext = FileValidator.extractSafeExt(file.getContentType());
        String key = props.getStudentCardDir() + "/" + UUID.randomUUID() + ext;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(props.getStudentCardBucket())
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(bytes)
        );

        log.info("학생증 업로드 완료: bucket={}, key={}", props.getStudentCardBucket(), key);
        return key;
    }

    // 퍼블릭 버킷 공통 업로드 (S3 public URL 반환)
    private String upload(MultipartFile file, String dir) throws IOException {
        // MIME 타입·매직바이트 검증 (게시글·채팅·프로필 이미지 공통)
        byte[] bytes = FileValidator.validatePostMedia(file);

        String ext = FileValidator.extractSafeExt(file.getContentType());
        String key = dir + "/" + UUID.randomUUID() + ext;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(props.getBucket())
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(bytes)
        );

        String url = String.format("https://%s.s3.%s.amazonaws.com/%s", props.getBucket(), props.getRegion(), key);
        log.info("파일 업로드 완료: bucket={}, key={}, url={}", props.getBucket(), key, url);
        return url;
    }

    // S3 URL에서 오브젝트 key 추출 (https://{bucket}.s3.{region}.amazonaws.com/{key})
    private String extractKeyFromUrl(String url) {
        int idx = url.indexOf(".amazonaws.com/");
        if (idx < 0) return null;
        return url.substring(idx + ".amazonaws.com/".length());
    }

}

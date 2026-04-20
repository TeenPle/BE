package com.shu.backend.global.file;

import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.media.exception.MediaException;
import com.shu.backend.domain.media.exception.status.MediaErrorStatus;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

// 배포 후 LocalFileStorageService -> S3FileStorageService 변경
@Slf4j
//@Service
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    // AWS S3 Client (S3Config에서 Bean 등록)
    private final S3Client s3Client;

    // 파일 스토리지 설정 값 (bucket, dir, base-url)
    private final FileStorageProperties props;

    /**
     * 학생증 이미지 업로드
     * - 실패 시 UserException 발생
     */
    @Override
    public String uploadStudentCardImage(MultipartFile file) {
        try {
            return upload(file, props.getStudentCardDir());
        } catch (Exception e) {
            log.error("Student card upload failed", e);
            throw new UserException(
                    UserErrorStatus.USER_STUDENT_CARD_UPLOAD_FAIL
            );
        }
    }

    /**
     * 채팅 이미지 업로드
     * - 실패 시 ChatMessageException 발생
     */
    @Override
    public String uploadChatImage(MultipartFile file) {
        try {
            return upload(file, props.getChatDir());
        } catch (Exception e) {
            log.error("Chat image upload failed", e);
            throw new ChatMessageException(
                    ChatMessageErrorStatus.CHAT_IMAGE_UPLOAD_FAIL
            );
        }
    }

    @Override
    public String uploadPostMedia(MultipartFile file) {
        try {
            return upload(file, props.getPostDir());
        } catch (Exception e) {
            log.error("Post media upload failed", e);
            throw new MediaException(MediaErrorStatus.POST_MEDIA_UPLOAD_FAIL);
        }
    }

    /**
     * 공통 S3 업로드 로직
     * - 예외는 상위 메서드에서 도메인 예외로 변환
     */
    private String upload(MultipartFile file, String dir) throws IOException {

        // 원본 파일명에서 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String ext = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // S3 객체 key 생성
        // 예) chat/uuid.jpg, student-card/uuid.png
        String key = dir + "/" + UUID.randomUUID() + ext;

        // S3 PutObject 요청 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(file.getContentType())
                .build();

        log.info("bucket = {}", props.getBucket());
        log.info("dir = {}", dir);
        log.info("key = {}", key);

        // S3 업로드 실행
        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(
                        file.getInputStream(),
                        file.getSize()
                )
        );

        // 업로드된 파일 접근 URL 반환
        return props.getBaseUrl() + "/" + key;
    }
}

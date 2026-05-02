package com.shu.backend.global.file;

import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.media.exception.MediaException;
import com.shu.backend.domain.media.exception.status.MediaErrorStatus;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

//@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload-dir.student-card}")
    private String studentCardUploadDir;

    @Value("${file.upload-dir.chat}")
    private String chatUploadDir;

    @Value("${file.upload-dir.post}")
    private String postUploadDir;

    @Value("${file.upload-dir.profile:uploads/profile}")
    private String profileUploadDir;

    @Value("${server.port}")
    private int serverPort;

    @Override
    public String uploadStudentCardImage(MultipartFile file) {
        return upload(file, studentCardUploadDir, "/uploads/student-card/",
                () -> new UserException(UserErrorStatus.USER_STUDENT_CARD_UPLOAD_FAIL));
    }

    @Override
    public String uploadChatImage(MultipartFile file) {
        return uploadChatImageFile(file).url();
    }

    @Override
    public StoredFile uploadChatImageFile(MultipartFile file) {
        return uploadStored(file, chatUploadDir, "/uploads/chat/",
                () -> new ChatMessageException(ChatMessageErrorStatus.CHAT_IMAGE_UPLOAD_FAIL));
    }

    @Override
    public String uploadPostMedia(MultipartFile file) {
        return upload(file, postUploadDir, "/uploads/post/",
                () -> new MediaException(MediaErrorStatus.POST_MEDIA_UPLOAD_FAIL));
    }

    @Override
    public String uploadProfileImage(MultipartFile file) {
        return upload(file, profileUploadDir, "/uploads/profile/",
                () -> new UserException(UserErrorStatus.USER_STUDENT_CARD_UPLOAD_FAIL));
    }

    @Override
    public void deleteStudentCardImage(String key) {
        // 로컬 환경에서는 삭제하지 않음 (개발용)
    }

    @Override
    public void deletePublicFile(String key) {
        // 로컬 환경에서는 삭제하지 않음 (개발용)
    }

    @Override
    public String generateStudentCardPresignedUrl(String key) {
        // 로컬 환경에서는 key를 그대로 반환 (개발용)
        return key;
    }

    private String upload(
            MultipartFile file,
            String uploadDir,
            String publicPath,
            java.util.function.Supplier<RuntimeException> onFail
    ) {
        String originalFilename = file.getOriginalFilename();
        String ext = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String storedFileName = UUID.randomUUID() + ext;

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetPath = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath);

            return "http://localhost:" + serverPort + publicPath + storedFileName;

        } catch (IOException e) {
            throw onFail.get();
        }
    }

    private StoredFile uploadStored(
            MultipartFile file,
            String uploadDir,
            String publicPath,
            java.util.function.Supplier<RuntimeException> onFail
    ) {
        String url = upload(file, uploadDir, publicPath, onFail);
        String key = publicPath.replaceFirst("^/uploads/", "").replaceFirst("^/", "") + url.substring(url.lastIndexOf('/') + 1);
        return new StoredFile("local", key, url, file.getContentType());
    }
}

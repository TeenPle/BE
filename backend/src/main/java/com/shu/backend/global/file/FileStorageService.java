package com.shu.backend.global.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadStudentCardImage(MultipartFile file);
    String uploadChatImage(MultipartFile file);
    StoredFile uploadChatImageFile(MultipartFile file);
    String uploadPostMedia(MultipartFile file);
    String uploadProfileImage(MultipartFile file);
    void deleteStudentCardImage(String key);
    void deletePublicFile(String key);
    String generateStudentCardPresignedUrl(String key);

    /** 퍼블릭 버킷에 저장된 URL(또는 key)을 presigned GET URL로 변환 */
    String toPresignedReadUrl(String storedUrl);
}

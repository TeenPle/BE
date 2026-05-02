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
}

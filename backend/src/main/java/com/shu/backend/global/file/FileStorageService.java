package com.shu.backend.global.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadStudentCardImage(MultipartFile file);
    String uploadChatImage(MultipartFile file);
    String uploadPostMedia(MultipartFile file);
    String uploadProfileImage(MultipartFile file);
    void deleteStudentCardImage(String key);
    String generateStudentCardPresignedUrl(String key);
}

package com.shu.backend.domain.chatmessage.service;

import com.shu.backend.domain.chatmessage.dto.ChatMessageDTO;
import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.media.entity.Media;
import com.shu.backend.domain.media.enums.MediaType;
import com.shu.backend.domain.media.repository.MediaRepository;
import com.shu.backend.domain.penalty.security.PenaltyChecker;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.global.file.FileStorageService;
import com.shu.backend.global.file.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatImageService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    private final FileStorageService fileStorageService;
    private final ChatImageModerationService moderationService;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final PenaltyChecker penaltyChecker;
    private final ChatActionRateLimiter chatActionRateLimiter;

    public ChatMessageDTO.UploadImageResponse upload(Long uploaderId, MultipartFile file) {
        if (!penaltyChecker.notPenalized(uploaderId)) {
            throw new ChatMessageException(ChatMessageErrorStatus.CHAT_PENALIZED);
        }
        chatActionRateLimiter.check(uploaderId, "image-upload", 5, 60);
        validateImageFile(file);

        StoredFile storedFile = fileStorageService.uploadChatImageFile(file);
        try {
            String moderationLabels = moderationService.validate(storedFile.bucket(), storedFile.key());
            User uploader = userRepository.getReferenceById(uploaderId);
            Media media = mediaRepository.save(Media.ofChatUpload(
                    storedFile.url(),
                    storedFile.key(),
                    MediaType.IMAGE,
                    uploader,
                    moderationLabels
            ));
            return new ChatMessageDTO.UploadImageResponse(media.getId(), media.getUrl());
        } catch (RuntimeException e) {
            fileStorageService.deletePublicFile(storedFile.key());
            throw e;
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES) {
            throw new ChatMessageException(ChatMessageErrorStatus.INVALID_CHAT_IMAGE);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ChatMessageException(ChatMessageErrorStatus.INVALID_CHAT_IMAGE);
        }
        try {
            if (ImageIO.read(file.getInputStream()) == null) {
                throw new ChatMessageException(ChatMessageErrorStatus.INVALID_CHAT_IMAGE);
            }
        } catch (IOException e) {
            throw new ChatMessageException(ChatMessageErrorStatus.INVALID_CHAT_IMAGE);
        }
    }
}

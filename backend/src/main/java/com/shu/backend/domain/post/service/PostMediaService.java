package com.shu.backend.domain.post.service;

import com.shu.backend.domain.media.entity.Media;
import com.shu.backend.domain.media.enums.MediaTargetType;
import com.shu.backend.domain.media.enums.MediaType;
import com.shu.backend.domain.media.exception.MediaException;
import com.shu.backend.domain.media.exception.status.MediaErrorStatus;
import com.shu.backend.domain.media.repository.MediaRepository;
import com.shu.backend.domain.post.dto.PostMediaResponse;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostMediaService {

    private final MediaRepository mediaRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public List<PostMediaResponse> uploadAndSave(Long postId, List<MultipartFile> files, User uploader) {
        return files.stream()
                .map(file -> {
                    String url = fileStorageService.uploadPostMedia(file);
                    MediaType mediaType = resolveMediaType(file);
                    Media media = Media.ofPost(url, postId, mediaType, uploader);
                    mediaRepository.save(media);
                    return PostMediaResponse.from(media);
                })
                .toList();
    }

    @Transactional
    public void deleteByIds(List<Long> mediaIds, Long postId, Long requesterId) {
        if (mediaIds == null || mediaIds.isEmpty()) return;

        List<Media> mediaList = mediaRepository.findAllById(mediaIds);

        for (Media media : mediaList) {
            if (!media.getTargetType().equals(MediaTargetType.POST) || !media.getTargetId().equals(postId)) {
                throw new MediaException(MediaErrorStatus.POST_MEDIA_NO_PERMISSION);
            }
            if (!media.getUploader().getId().equals(requesterId)) {
                throw new MediaException(MediaErrorStatus.POST_MEDIA_NO_PERMISSION);
            }
        }

        mediaList.forEach(m -> fileStorageService.deletePublicFile(m.getUrl()));
        mediaRepository.deleteAll(mediaList);
    }

    @Transactional
    public void deleteAllByPostId(Long postId) {
        List<Media> mediaList = mediaRepository.findByTargetTypeAndTargetId(MediaTargetType.POST, postId);
        mediaList.forEach(m -> fileStorageService.deletePublicFile(m.getUrl()));
        mediaRepository.deleteAll(mediaList);
    }

    @Transactional(readOnly = true)
    public List<PostMediaResponse> getByPostId(Long postId) {
        return mediaRepository.findByTargetTypeAndTargetId(MediaTargetType.POST, postId)
                .stream()
                .map(PostMediaResponse::from)
                .toList();
    }

    private MediaType resolveMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.startsWith("image/")) return MediaType.IMAGE;
            if (contentType.startsWith("video/")) return MediaType.VIDEO;
        }
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp")) {
                return MediaType.IMAGE;
            }
            if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi")) {
                return MediaType.VIDEO;
            }
        }
        return MediaType.DOCUMENT;
    }
}

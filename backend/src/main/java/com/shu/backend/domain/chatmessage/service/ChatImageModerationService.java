package com.shu.backend.domain.chatmessage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.ModerationLabel;
import software.amazon.awssdk.services.rekognition.model.S3Object;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatImageModerationService {

    private static final Set<String> BLOCKED_LABELS = Set.of(
            "Explicit Nudity",
            "Sexual Activity",
            "Graphic Male Nudity",
            "Graphic Female Nudity",
            "Sexual Situations",
            "Graphic Violence Or Gore",
            "Graphic Violence",
            "Visually Disturbing",
            "Drugs",
            "Weapons",
            "Hate Symbols"
    );

    private final RekognitionClient rekognitionClient;
    private final ObjectMapper objectMapper;

    @Value("${chat.image.moderation.min-confidence:80}")
    private float minConfidence;

    public String validate(String bucket, String key) {
        try {
            DetectModerationLabelsResponse response = rekognitionClient.detectModerationLabels(
                    DetectModerationLabelsRequest.builder()
                            .image(Image.builder()
                                    .s3Object(S3Object.builder()
                                            .bucket(bucket)
                                            .name(key)
                                            .build())
                                    .build())
                            .minConfidence(minConfidence)
                            .build()
            );

            List<ModerationLabel> labels = response.moderationLabels();
            boolean rejected = labels.stream().anyMatch(label ->
                    BLOCKED_LABELS.contains(label.name())
                            || (label.parentName() != null && BLOCKED_LABELS.contains(label.parentName()))
            );

            if (rejected) {
                log.info("채팅 이미지 moderation 차단: bucket={}, key={}, labels={}", bucket, key, labels);
                throw new ChatMessageException(ChatMessageErrorStatus.CHAT_IMAGE_REJECTED);
            }

            return serializeLabels(labels);
        } catch (ChatMessageException e) {
            throw e;
        } catch (Exception e) {
            log.warn("채팅 이미지 moderation 실패: bucket={}, key={}", bucket, key, e);
            throw new ChatMessageException(ChatMessageErrorStatus.CHAT_IMAGE_REJECTED);
        }
    }

    private String serializeLabels(List<ModerationLabel> labels) {
        try {
            return objectMapper.writeValueAsString(labels.stream()
                    .map(label -> new ModerationResult(
                            label.name(),
                            label.parentName(),
                            label.confidence()
                    ))
                    .toList());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private record ModerationResult(String name, String parentName, Float confidence) {
    }
}

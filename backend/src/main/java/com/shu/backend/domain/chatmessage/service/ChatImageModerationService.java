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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatImageModerationService {

    private final RekognitionClient rekognitionClient;
    private final ObjectMapper objectMapper;

    @Value("${chat.image.moderation.min-confidence:50}")
    private float minConfidence;

    @Value("${moderation.image.nudity-threshold:50}")
    private float nudityThreshold;

    @Value("${moderation.image.suggestive-threshold:80}")
    private float suggestiveThreshold;

    @Value("${moderation.image.violence-threshold:70}")
    private float violenceThreshold;

    @Value("${moderation.image.disturbing-threshold:70}")
    private float disturbingThreshold;

    @Value("${moderation.image.drugs-threshold:70}")
    private float drugsThreshold;

    @Value("${moderation.image.hate-threshold:70}")
    private float hateThreshold;

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
            ModerationLabel violated = findViolation(labels);

            if (violated != null) {
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

    private ModerationLabel findViolation(List<ModerationLabel> labels) {
        Map<String, Float> thresholds = buildThresholdMap();

        for (ModerationLabel label : labels) {
            String topCategory = getTopCategory(label.name(), label.parentName());
            Float threshold = thresholds.get(topCategory);
            if (threshold != null && label.confidence() >= threshold) {
                return label;
            }
        }
        return null;
    }

    private String getTopCategory(String labelName, String parentName) {
        String combined = ((labelName == null ? "" : labelName) + " " + (parentName == null ? "" : parentName));
        if (combined.contains("Nudity") || combined.contains("nudity")) return "Explicit Nudity";
        if (combined.contains("Suggestive") || combined.contains("Swimwear")
                || combined.contains("Underwear") || combined.contains("Revealing")) return "Suggestive";
        if (combined.contains("Violence") || combined.contains("Weapon")
                || combined.contains("Blood") || combined.contains("Gore")) return "Violence";
        if (combined.contains("Visually Disturbing") || combined.contains("Emaciated")
                || combined.contains("Self Injury") || combined.contains("Corpse")) return "Visually Disturbing";
        if (combined.contains("Drug") || combined.contains("Tobacco")
                || combined.contains("Smoking")) return "Drugs & Tobacco";
        if (combined.contains("Hate Symbol") || combined.contains("Nazi")
                || combined.contains("White Supremacy")) return "Hate Symbols";
        return labelName;
    }

    private Map<String, Float> buildThresholdMap() {
        return Map.of(
                "Explicit Nudity", nudityThreshold,
                "Suggestive", suggestiveThreshold,
                "Violence", violenceThreshold,
                "Visually Disturbing", disturbingThreshold,
                "Drugs & Tobacco", drugsThreshold,
                "Hate Symbols", hateThreshold
        );
    }

    private record ModerationResult(String name, String parentName, Float confidence) {
    }
}

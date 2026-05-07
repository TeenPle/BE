package com.shu.backend.global.moderation;

import com.shu.backend.domain.media.exception.MediaException;
import com.shu.backend.domain.media.exception.status.MediaErrorStatus;
import com.shu.backend.global.file.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AWS Rekognition을 이용한 이미지 유해 콘텐츠 자동 탐지 서비스.
 *
 * 탐지 카테고리 및 차단 임계값 (application.yml에서 조정 가능):
 *  - Explicit Nudity  : 50% 이상 → 차단
 *  - Suggestive       : 80% 이상 → 차단
 *  - Violence         : 70% 이상 → 차단
 *  - Visually Disturbing (자해 등) : 70% 이상 → 차단
 *  - Drugs & Tobacco  : 70% 이상 → 차단
 *  - Hate Symbols     : 70% 이상 → 차단
 *
 * 동작 방식:
 *  - 파일 크기 ≤ 5MB : 바이트를 Rekognition에 직접 전송 (S3 업로드 불필요)
 *  - 파일 크기 > 5MB : S3 임시 경로에 업로드 → Rekognition S3 참조 분석 → 검사 후 즉시 삭제
 *  - Rekognition 검사 실패 시 안전을 위해 업로드 차단
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageModerationService {

    private static final long REKOGNITION_BYTES_LIMIT = 5 * 1024 * 1024L; // 5MB
    private static final String TEMP_DIR = "temp-moderation";

    private final RekognitionClient rekognitionClient;
    private final S3Client s3Client;
    private final FileStorageProperties props;

    // ─── 카테고리별 임계값 (application.yml) ───────────────────────────────────
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

    /**
     * 이미지 파일을 Rekognition으로 검사한다.
     * 부적절한 이미지면 MediaException(INAPPROPRIATE_IMAGE)을 던진다.
     * 이미지가 아닌 파일(VIDEO 등)은 검사를 건너뛴다.
     */
    public void check(MultipartFile file) {
        if (!isImage(file)) return;

        try {
            List<ModerationLabel> labels = file.getSize() <= REKOGNITION_BYTES_LIMIT
                    ? detectWithBytes(file)
                    : detectWithS3(file);

            ModerationLabel violated = findViolation(labels);
            if (violated != null) {
                log.warn("부적절한 이미지 탐지: name={}, label={}, confidence={}",
                        file.getOriginalFilename(), violated.name(), violated.confidence());
                throw new MediaException(MediaErrorStatus.INAPPROPRIATE_IMAGE);
            }

        } catch (MediaException e) {
            throw e;
        } catch (Exception e) {
            log.error("Rekognition 이미지 검사 실패 (업로드 차단): file={}, error={}",
                    file.getOriginalFilename(), e.getMessage());
            throw new MediaException(MediaErrorStatus.INAPPROPRIATE_IMAGE);
        }
    }

    // ─── 5MB 이하: 바이트 직접 전송 ─────────────────────────────────────────────

    private List<ModerationLabel> detectWithBytes(MultipartFile file) throws IOException {
        SdkBytes imageBytes = SdkBytes.fromByteArray(file.getBytes());

        DetectModerationLabelsRequest request = DetectModerationLabelsRequest.builder()
                .image(Image.builder().bytes(imageBytes).build())
                .minConfidence(50f) // 최저 반환 신뢰도 (임계값 이하는 필터에서 다시 걸러냄)
                .build();

        return rekognitionClient.detectModerationLabels(request).moderationLabels();
    }

    // ─── 5MB 초과: S3 임시 업로드 후 분석, 즉시 삭제 ────────────────────────────

    private List<ModerationLabel> detectWithS3(MultipartFile file) throws IOException {
        String tempKey = TEMP_DIR + "/" + UUID.randomUUID();

        try {
            // 임시 업로드
            s3Client.putObject(
                    software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                            .bucket(props.getBucket())
                            .key(tempKey)
                            .contentType(file.getContentType())
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes())
            );

            // S3 참조로 Rekognition 분석
            DetectModerationLabelsRequest request = DetectModerationLabelsRequest.builder()
                    .image(Image.builder()
                            .s3Object(S3Object.builder()
                                    .bucket(props.getBucket())
                                    .name(tempKey)
                                    .build())
                            .build())
                    .minConfidence(50f)
                    .build();

            return rekognitionClient.detectModerationLabels(request).moderationLabels();

        } finally {
            // 분석 결과에 관계없이 반드시 임시 파일 삭제
            deleteTempFile(tempKey);
        }
    }

    // ─── 위반 라벨 탐색 ─────────────────────────────────────────────────────────

    private ModerationLabel findViolation(List<ModerationLabel> labels) {
        // 카테고리별 임계값 매핑
        Map<String, Float> thresholds = buildThresholdMap();

        for (ModerationLabel label : labels) {
            String topCategory = getTopCategory(label.name());
            Float threshold = thresholds.get(topCategory);
            if (threshold != null && label.confidence() >= threshold) {
                return label;
            }
        }
        return null;
    }

    /**
     * Rekognition 라벨 이름에서 최상위 카테고리 추출.
     * 예: "Explicit Nudity" → "Explicit Nudity"
     *     "Female Swimwear Or Underwear" → "Suggestive"  (부모 라벨이 Suggestive)
     */
    private String getTopCategory(String labelName) {
        // Rekognition은 상위/하위 계층 라벨을 모두 반환하므로, 상위 6개 카테고리만 체크
        if (labelName.contains("Nudity") || labelName.contains("nudity")) return "Explicit Nudity";
        if (labelName.equals("Suggestive") || labelName.contains("Swimwear")
                || labelName.contains("Underwear") || labelName.contains("Revealing")) return "Suggestive";
        if (labelName.contains("Violence") || labelName.contains("Weapon")
                || labelName.contains("Blood")) return "Violence";
        if (labelName.contains("Visually Disturbing") || labelName.contains("Emaciated")
                || labelName.contains("Self Injury") || labelName.contains("Corpse")) return "Visually Disturbing";
        if (labelName.contains("Drug") || labelName.contains("Tobacco")
                || labelName.contains("Smoking")) return "Drugs & Tobacco";
        if (labelName.contains("Hate Symbol") || labelName.contains("Nazi")
                || labelName.contains("White Supremacy")) return "Hate Symbols";
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

    // ─── 유틸 ────────────────────────────────────────────────────────────────────

    private boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    private void deleteTempFile(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build());
        } catch (Exception e) {
            log.warn("Rekognition 임시 파일 삭제 실패: key={}", key);
        }
    }
}

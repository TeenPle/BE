package com.shu.backend.domain.contentfilter.entity;

import com.shu.backend.domain.contentfilter.enums.ContentFilterCategory;
import com.shu.backend.domain.contentfilter.enums.ContentFilterSeverity;
import com.shu.backend.domain.contentfilter.enums.ContentFilterSource;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "content_filter_term",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_content_filter_term_language_normalized",
                        columnNames = {"language", "normalized_term"}
                )
        },
        indexes = {
                @Index(name = "idx_content_filter_term_enabled_language", columnList = "enabled, language"),
                @Index(name = "idx_content_filter_term_source", columnList = "source, source_key")
        }
)
public class ContentFilterTerm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(nullable = false, length = 255)
    private String term;

    @Column(name = "normalized_term", nullable = false, length = 255)
    private String normalizedTerm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentFilterSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ContentFilterCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ContentFilterSource source;

    @Column(name = "source_key", length = 500)
    private String sourceKey;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    public void refresh(
            String term,
            String normalizedTerm,
            ContentFilterSeverity severity,
            ContentFilterCategory category,
            ContentFilterSource source,
            String sourceKey
    ) {
        this.term = term;
        this.normalizedTerm = normalizedTerm;
        this.severity = severity;
        this.category = category;
        this.source = source;
        this.sourceKey = sourceKey;
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}

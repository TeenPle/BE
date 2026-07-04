package com.shu.backend.global.moderation;

import com.shu.backend.domain.contentfilter.entity.ContentFilterTerm;
import com.shu.backend.domain.contentfilter.enums.ContentFilterSeverity;
import com.shu.backend.domain.contentfilter.repository.ContentFilterTermRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class ContentFilterDictionaryService {

    private final ContentFilterTermRepository termRepository;
    private final AtomicReference<List<ContentFilterTerm>> activeTerms = new AtomicReference<>(List.of());

    @PostConstruct
    @Transactional(readOnly = true)
    public void reload() {
        List<ContentFilterTerm> terms = termRepository.findByEnabledTrue().stream()
                .filter(term -> term.getSeverity() == ContentFilterSeverity.BLOCK)
                .filter(term -> term.getNormalizedTerm() != null && !term.getNormalizedTerm().isBlank())
                .sorted(Comparator.comparingInt((ContentFilterTerm term) -> term.getNormalizedTerm().length()).reversed())
                .toList();
        activeTerms.set(terms);
    }

    public ContentFilterMatch findBlockedTerm(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return null;
        }

        for (ContentFilterTerm term : activeTerms.get()) {
            if (normalizedText.contains(term.getNormalizedTerm())) {
                return new ContentFilterMatch(term.getTerm(), term.getSeverity(), term.getCategory());
            }
        }
        return null;
    }
}

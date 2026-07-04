package com.shu.backend.global.moderation;

import com.shu.backend.domain.contentfilter.entity.ContentFilterTerm;
import com.shu.backend.domain.contentfilter.enums.ContentFilterSeverity;
import com.shu.backend.domain.contentfilter.repository.ContentFilterTermRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
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
        if (terms.isEmpty()) {
            log.warn("Content filter dictionary is empty. Text filtering will only use built-in pattern checks until sync succeeds.");
        } else {
            log.info("Content filter dictionary loaded. activeBlockTerms={}", terms.size());
        }
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

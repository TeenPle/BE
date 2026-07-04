package com.shu.backend.domain.contentfilter.repository;

import com.shu.backend.domain.contentfilter.entity.ContentFilterTerm;
import com.shu.backend.domain.contentfilter.enums.ContentFilterSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContentFilterTermRepository extends JpaRepository<ContentFilterTerm, Long> {

    List<ContentFilterTerm> findByEnabledTrue();

    Optional<ContentFilterTerm> findByLanguageAndNormalizedTerm(String language, String normalizedTerm);

    List<ContentFilterTerm> findBySourceAndSourceKeyAndLanguageAndNormalizedTermNotIn(
            ContentFilterSource source,
            String sourceKey,
            String language,
            Collection<String> normalizedTerms
    );
}

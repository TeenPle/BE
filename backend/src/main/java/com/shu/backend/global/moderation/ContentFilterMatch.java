package com.shu.backend.global.moderation;

import com.shu.backend.domain.contentfilter.enums.ContentFilterCategory;
import com.shu.backend.domain.contentfilter.enums.ContentFilterSeverity;

public record ContentFilterMatch(
        String term,
        ContentFilterSeverity severity,
        ContentFilterCategory category
) {
}

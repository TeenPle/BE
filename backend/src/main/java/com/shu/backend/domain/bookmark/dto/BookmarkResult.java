package com.shu.backend.domain.bookmark.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BookmarkResult {
    private final boolean bookmarked;
}

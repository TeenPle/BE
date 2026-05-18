package com.shu.backend.global.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequestUtils {

    private static final int DEFAULT_MAX_SIZE = 50;

    private PageRequestUtils() {
    }

    public static Pageable of(int page, int size) {
        return of(page, size, DEFAULT_MAX_SIZE);
    }

    public static Pageable of(int page, int size, int maxSize) {
        return PageRequest.of(safePage(page), safeSize(size, maxSize));
    }

    public static Pageable of(int page, int size, int maxSize, Sort sort) {
        return PageRequest.of(safePage(page), safeSize(size, maxSize), sort);
    }

    public static Pageable sanitize(Pageable pageable, int maxSize) {
        Sort sort = pageable.getSortOr(Sort.unsorted());
        return PageRequest.of(safePage(pageable.getPageNumber()), safeSize(pageable.getPageSize(), maxSize), sort);
    }

    public static Pageable slice(Pageable pageable) {
        Sort sort = pageable.getSortOr(Sort.unsorted());
        return new OffsetLimitPageRequest(pageable.getOffset(), pageable.getPageSize() + 1, sort);
    }

    public static int safePage(int page) {
        return Math.max(page, 0);
    }

    public static int safeSize(int size, int maxSize) {
        int upperBound = Math.max(maxSize, 1);
        return Math.min(Math.max(size, 1), upperBound);
    }
}

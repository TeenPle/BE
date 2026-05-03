package com.shu.backend.global.file;

public record StoredFile(
        String bucket,
        String key,
        String url,
        String contentType
) {
}

package com.shu.backend.domain.report.enums;

public enum ReportStatus {
    PENDING,
    RESOLVED,   // 제재 적용
    REJECTED,   // 신고 거절
    WARNED      // 경고 발령
}

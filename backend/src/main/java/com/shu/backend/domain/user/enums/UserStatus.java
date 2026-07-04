package com.shu.backend.domain.user.enums;

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    /** 탈퇴 요청 후 7일 유예 기간 중인 상태. 기간 내 복구 가능, 만료 시 스케줄러가 비식별 처리 후 DELETED로 전환. */
    PENDING_DELETION,
    DELETED
}

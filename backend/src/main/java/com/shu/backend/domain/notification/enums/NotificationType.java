package com.shu.backend.domain.notification.enums;

/**
 * 알림의 종류.
 *
 * DB에는 문자열(EnumType.STRING)로 저장되고, 푸시 data 페이로드의 "type" 값으로도
 * 그대로 내려가므로 프론트엔드(fcm_service.dart / notification_page.dart)의
 * 타입 분기 문자열과 반드시 일치해야 한다. 이름 변경 시 양쪽 모두 수정할 것.
 */
public enum NotificationType {

    // ===== 커뮤니티 활동 알림 (사용자 설정으로 끌 수 있음) ===== //
    COMMENT,                // 내 게시글에 새 댓글
    REPLY,                  // 내 댓글에 새 대댓글
    POST_LIKE,              // 내 게시글 좋아요
    COMMENT_LIKE,           // 내 댓글 좋아요
    CHAT,                   // 새 채팅 메시지

    // ===== 운영 알림 (놓치면 안 되므로 설정과 무관하게 발송) ===== //
    INQUIRY,                // 내 문의에 답변 등록
    WARNING,                // 관리자 경고 발령
    PENALTY,                // 커뮤니티 이용 제재
    VERIFICATION_APPROVED,  // 학교 인증 승인
    VERIFICATION_REJECTED,  // 학교 인증 거절
    SYSTEM                  // 기타 시스템 알림
}

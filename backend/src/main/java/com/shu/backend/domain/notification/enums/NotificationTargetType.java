package com.shu.backend.domain.notification.enums;

/**
 * 알림이 가리키는 대상 엔티티의 종류. targetId와 짝을 이뤄
 * 프론트엔드가 알림 탭 시 어느 상세 페이지로 이동할지 결정하는 데 사용한다.
 *
 * DB에 문자열로 저장되고 푸시 data의 "targetType" 값으로도 내려가므로
 * 프론트엔드 분기 문자열과 일치해야 한다.
 */
public enum NotificationTargetType {
    POST,                  // 게시글 (targetId = postId)
    COMMENT,               // 댓글 (targetId = commentId)
    CHAT_MSG,              // 채팅 (targetId = roomId)
    INQUIRY,               // 문의 (targetId = inquiryId)
    WARNING,               // 경고 (targetId = warningId)
    PENALTY,               // 제재 (targetId = penaltyId)
    VERIFICATION_REQUEST   // 학교 인증 요청 (targetId = requestId)
}

package com.team.independence.ai.summary.model;

/** 상담 채팅 메시지의 발화자. 사람 상담사 ↔ 청년(사용자) 1:1 대화. */
public enum SenderType {
    /** 청년 (상담받는 사용자) */
    USER,
    /** 사람 상담사 */
    COUNSELOR
}

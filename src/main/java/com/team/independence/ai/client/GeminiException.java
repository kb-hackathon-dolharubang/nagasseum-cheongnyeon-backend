package com.team.independence.ai.client;

/**
 * Gemini 호출 실패(전송 오류, HTTP 오류, 응답 껍데기 파싱 실패, 재시도 소진).
 * 호출 측(서비스)이 이 예외를 잡아 도메인 {@code BusinessException} 으로 변환한다.
 */
public class GeminiException extends RuntimeException {

    public GeminiException(String message) {
        super(message);
    }

    public GeminiException(String message, Throwable cause) {
        super(message, cause);
    }
}

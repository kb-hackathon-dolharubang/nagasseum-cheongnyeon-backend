package com.team.independence.ai.client;

import java.util.Map;

/**
 * 단일 LLM 호출 래퍼. 이 프로젝트의 AI 기능은 전부 "조회는 우리 코드가 다 하고
 * 프롬프트에 넣어준 뒤 한 번 호출" 이므로 인터페이스도 메서드 하나다.
 */
public interface GeminiClient {

    /**
     * 구조화(JSON) 출력을 강제해 한 번 호출하고, 모델이 낸 JSON 문자열을 그대로 돌려준다.
     *
     * @param systemInstruction 역할·금지사항 등 시스템 지시
     * @param userPrompt        신청인 정보 + 요건이 채워진 사용자 프롬프트
     * @param responseSchema    Gemini {@code generationConfig.responseSchema} 에 넣을 스키마
     *                          (타입은 대문자: OBJECT/ARRAY/STRING ...)
     * @return 모델 응답 후보의 text (JSON 문자열)
     * @throws GeminiException 전송/HTTP/껍데기 파싱 실패, 재시도 소진
     */
    String generateJson(String systemInstruction, String userPrompt, Map<String, Object> responseSchema);
}

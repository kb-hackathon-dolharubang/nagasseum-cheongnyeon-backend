package com.team.independence.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 테스트 프로필용 더미. 실제 네트워크 없이 스키마에 맞는 최소 JSON 을 돌려준다.
 * 서비스 로직 단위 테스트는 Mockito 로 {@link GeminiClient} 를 직접 목킹하므로 이 빈을 쓰지 않는다.
 * 전체 컨텍스트({@code @ActiveProfiles("test")}) 로딩 시 빈 충돌/누락을 막기 위한 것.
 */
@Slf4j
@Component
@Profile("test")
public class GeminiStubClient implements GeminiClient {

    @Override
    public String generateJson(String systemInstruction, String userPrompt, Map<String, Object> responseSchema) {
        log.debug("[GeminiStub] generateJson 호출 - 더미 응답 반환");
        return "{\"advice\":\"(stub) 실제 AI 응답 아님\"}";
    }
}

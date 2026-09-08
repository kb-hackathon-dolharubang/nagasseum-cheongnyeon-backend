package com.team.independence.ai.client;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Gemini(Generative Language API) 연동 설정.
 *
 * <p>이 프로젝트는 {@code .env} 를 {@code PropertySourcesPlaceholderConfigurer} 로 읽어
 * {@code @Value} 로 주입한다. SDK 의 {@code fromEnv()} 자동 감지를 쓰지 않고 키를 명시적으로 주입한다.
 * 그래서 공식 {@code google-genai} SDK 대신 {@code RestTemplate} 로 REST 를 직접 호출한다
 * (구식 Spring 5.3 / Jackson 2.x 스택과의 트랜지티브 충돌 회피, CODEF 연동과 동일한 패턴).
 */
@Getter
@Component
public class GeminiProperties {

    /** Google AI Studio 에서 발급한 API 키. (.env: GEMINI_API_KEY) */
    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    /** 사용할 모델명. 무료 티어 + 구조화 출력(responseSchema) 지원 모델. */
    @Value("${GEMINI_MODEL:gemini-3.6-flash}")
    private String model;

    /** generateContent 엔드포인트 베이스. */
    @Value("${GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    /** 읽기 타임아웃(ms). */
    @Value("${GEMINI_TIMEOUT_MS:30000}")
    private int timeoutMs;

    /** 429 / 5xx / 타임아웃 시 재시도 횟수(최초 시도 제외). */
    @Value("${GEMINI_MAX_RETRIES:3}")
    private int maxRetries;

    /** 샘플링 온도. 심사 일관성을 위해 낮게. */
    @Value("${GEMINI_TEMPERATURE:0.2}")
    private double temperature;
}

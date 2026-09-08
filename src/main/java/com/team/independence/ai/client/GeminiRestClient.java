package com.team.independence.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.independence.ai.client.dto.GeminiGenerateRequest;
import com.team.independence.ai.client.dto.GeminiGenerateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Gemini generateContent REST 호출.
 *
 * <p>재시도: 429 / 5xx / 타임아웃만 지수 백오프로 재시도한다. 그 외 4xx(잘못된 요청·인증 등)는
 * 재시도해도 같으므로 즉시 {@link GeminiException}.
 *
 * <p>API 키는 URL 쿼리스트링이 아니라 {@code x-goog-api-key} 헤더로 보낸다(로그·프록시 노출 최소화).
 */
@Slf4j
@Component
@Profile("!test")
public class GeminiRestClient implements GeminiClient {

    private final GeminiProperties props;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public GeminiRestClient(GeminiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(props.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String generateJson(String systemInstruction, String userPrompt, Map<String, Object> responseSchema) {
        String url = props.getBaseUrl() + "/models/" + props.getModel() + ":generateContent";

        GeminiGenerateRequest body = GeminiGenerateRequest.builder()
                .systemInstruction(new GeminiGenerateRequest.Content(
                        null, List.of(new GeminiGenerateRequest.Part(systemInstruction))))
                .contents(List.of(new GeminiGenerateRequest.Content(
                        "user", List.of(new GeminiGenerateRequest.Part(userPrompt)))))
                .generationConfig(GeminiGenerateRequest.GenerationConfig.builder()
                        .temperature(props.getTemperature())
                        .responseMimeType("application/json")
                        .responseSchema(responseSchema)
                        .build())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", props.getApiKey());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new GeminiException("요청 직렬화 실패", e);
        }
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        int attempts = props.getMaxRetries() + 1;
        RuntimeException last = null;
        for (int i = 0; i < attempts; i++) {
            if (i > 0) sleepBackoff(i);
            try {
                ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                return extractText(res.getBody());
            } catch (HttpStatusCodeException e) {
                int sc = e.getRawStatusCode();
                boolean retryable = sc == 429 || sc >= 500;
                log.warn("Gemini HTTP {} (attempt {}/{}) {}", sc, i + 1, attempts,
                        truncate(e.getResponseBodyAsString()));
                if (!retryable) {
                    throw new GeminiException("Gemini HTTP " + sc + ": " + truncate(e.getResponseBodyAsString()), e);
                }
                last = new GeminiException("Gemini HTTP " + sc + " (재시도 소진)", e);
            } catch (ResourceAccessException e) {
                log.warn("Gemini 타임아웃/전송오류 (attempt {}/{}): {}", i + 1, attempts, e.getMessage());
                last = new GeminiException("Gemini 전송 오류 (재시도 소진)", e);
            }
        }
        throw last != null ? last : new GeminiException("Gemini 호출 실패");
    }

    private String extractText(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new GeminiException("Gemini 응답 본문이 비어 있음");
        }
        GeminiGenerateResponse parsed;
        try {
            parsed = objectMapper.readValue(rawBody, GeminiGenerateResponse.class);
        } catch (Exception e) {
            throw new GeminiException("Gemini 응답 껍데기 파싱 실패: " + truncate(rawBody), e);
        }
        if (parsed.getPromptFeedback() != null && parsed.getPromptFeedback().getBlockReason() != null) {
            throw new GeminiException("Gemini 프롬프트 차단: " + parsed.getPromptFeedback().getBlockReason());
        }
        String text = parsed.firstText();
        if (text == null || text.isBlank()) {
            throw new GeminiException("Gemini 응답에 후보 text 없음 (finishReason="
                    + parsed.firstFinishReason() + ")");
        }
        return text;
    }

    private void sleepBackoff(int attemptIndex) {
        long ms = 500L * (1L << (attemptIndex - 1)); // 500, 1000, 2000, ...
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new GeminiException("재시도 대기 중 인터럽트", ie);
        }
    }

    private String truncate(String s) {
        if (s == null) return "null";
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}

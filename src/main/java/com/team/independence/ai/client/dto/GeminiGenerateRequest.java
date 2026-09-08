package com.team.independence.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * {@code POST {baseUrl}/models/{model}:generateContent} 요청 바디.
 * <pre>
 * {
 *   "systemInstruction": { "parts": [ { "text": "..." } ] },
 *   "contents":          [ { "role": "user", "parts": [ { "text": "..." } ] } ],
 *   "generationConfig":  { "temperature": 0.2,
 *                          "responseMimeType": "application/json",
 *                          "responseSchema": { ... } }
 * }
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiGenerateRequest {

    private final Content systemInstruction;
    private final List<Content> contents;
    private final GenerationConfig generationConfig;

    @Getter
    @AllArgsConstructor
    public static class Content {
        private final String role; // systemInstruction 에서는 생략(null), contents 에서는 "user"
        private final List<Part> parts;
    }

    @Getter
    @AllArgsConstructor
    public static class Part {
        private final String text;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GenerationConfig {
        private final Double temperature;
        private final String responseMimeType;
        private final Map<String, Object> responseSchema;
    }
}

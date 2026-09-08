package com.team.independence.ai.eligibility.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.independence.ai.client.GeminiClient;
import com.team.independence.ai.client.GeminiException;
import com.team.independence.ai.eligibility.model.RuleFinding;
import com.team.independence.ai.eligibility.model.UserProfile;
import com.team.independence.ai.eligibility.policy.PolicyDefinition;
import com.team.independence.common.exception.BusinessException;
import com.team.independence.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 조언(advice) 생성을 LLM 에 맡기는 부분.
 * 프롬프트 조립 → {@link GeminiClient} 호출 → JSON 파싱 → advice 문자열.
 * 어떤 단계든 실패하면 {@code POLICY_AI_ASSESSMENT_FAILED} 로 던진다 (설계상 LLM 실패는 에러).
 */
@Slf4j
@Component
public class LlmEligibilityAssessor {

    private final GeminiClient geminiClient;
    private final EligibilityPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public LlmEligibilityAssessor(GeminiClient geminiClient,
                                  EligibilityPromptBuilder promptBuilder,
                                  ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    /** 조언 문구를 만든다. */
    public String generateAdvice(PolicyDefinition policy, UserProfile profile,
                                 List<RuleFinding> coreFindings, LocalDate asOf) {
        String prompt = promptBuilder.build(policy, profile, coreFindings, asOf);

        String raw;
        try {
            raw = geminiClient.generateJson(
                    EligibilityPromptBuilder.SYSTEM_INSTRUCTION, prompt, responseSchema());
        } catch (GeminiException e) {
            log.error("LLM 조언 호출 실패: policy={}", policy.id(), e);
            throw new BusinessException(ErrorCode.POLICY_AI_ASSESSMENT_FAILED, "AI 호출 실패: " + e.getMessage());
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            String advice = root.path("advice").asText(null);
            if (advice == null || advice.isBlank()) {
                throw new IllegalArgumentException("advice 필드 없음/빈 값");
            }
            return advice.trim();
        } catch (Exception e) {
            log.error("LLM 조언 파싱 실패: policy={}, raw={}", policy.id(), truncate(raw), e);
            throw new BusinessException(ErrorCode.POLICY_AI_ASSESSMENT_FAILED, "AI 응답 형식 오류");
        }
    }

    /** Gemini generationConfig.responseSchema (타입은 대문자). */
    public static Map<String, Object> responseSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "advice", Map.of("type", "STRING")),
                "required", List.of("advice"));
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}

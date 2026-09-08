package com.team.independence.ai.summary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.independence.ai.client.GeminiClient;
import com.team.independence.ai.client.GeminiException;
import com.team.independence.ai.summary.dto.SummaryReport;
import com.team.independence.ai.summary.dto.SummaryRequest;
import com.team.independence.common.exception.BusinessException;
import com.team.independence.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 채팅 요약 오케스트레이션: 프롬프트 조립 → 단일 LLM 호출 → 구조화 JSON 파싱 → SummaryReport.
 *
 * <p>단일 호출만 한다(map-reduce 미도입). 대화가 모델 컨텍스트를 넘길 만큼 길어지면 그때 분할을 추가한다.
 * 어떤 단계든 실패하면 {@code SUMMARY_AI_FAILED} 로 던진다.
 */
@Slf4j
@Service
public class ChatSummaryServiceImpl implements ChatSummaryService {

    private final GeminiClient geminiClient;
    private final SummaryPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public ChatSummaryServiceImpl(GeminiClient geminiClient,
                                  SummaryPromptBuilder promptBuilder,
                                  ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public SummaryReport summarize(SummaryRequest request) {
        String prompt = promptBuilder.build(request);

        String raw;
        try {
            raw = geminiClient.generateJson(
                    SummaryPromptBuilder.SYSTEM_INSTRUCTION, prompt, responseSchema());
        } catch (GeminiException e) {
            log.error("채팅 요약 LLM 호출 실패", e);
            throw new BusinessException(ErrorCode.SUMMARY_AI_FAILED, "AI 호출 실패: " + e.getMessage());
        }

        try {
            SummaryReport report = parse(raw);
            log.info("채팅 요약 완료: messages={}", request.getMessages().size());
            return report;
        } catch (Exception e) {
            log.error("채팅 요약 응답 파싱 실패: raw={}", truncate(raw), e);
            throw new BusinessException(ErrorCode.SUMMARY_AI_FAILED, "AI 응답 형식 오류");
        }
    }

    private SummaryReport parse(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);

        String summary = root.path("summary").asText(null);
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary 필드 없음/빈 값");
        }

        List<String> conditions = new ArrayList<>();
        JsonNode dc = root.path("discussedConditions");
        if (dc.isArray()) {
            for (JsonNode n : dc) {
                String s = n.asText(null);
                if (s != null && !s.isBlank()) conditions.add(s.trim());
            }
        }

        List<SummaryReport.ActionItem> actionItems = new ArrayList<>();
        JsonNode ai = root.path("actionItems");
        if (ai.isArray()) {
            for (JsonNode n : ai) {
                String who = n.path("who").asText(null);
                String task = n.path("task").asText(null);
                if (task != null && !task.isBlank()) {
                    actionItems.add(SummaryReport.ActionItem.builder()
                            .who(who).task(task.trim()).build());
                }
            }
        }

        return SummaryReport.builder()
                .summary(summary.trim())
                .housingGoal(blankToNull(root.path("housingGoal").asText(null)))
                .discussedConditions(conditions)
                .actionItems(actionItems)
                .nextSteps(blankToNull(root.path("nextSteps").asText(null)))
                .build();
    }

    /** Gemini generationConfig.responseSchema. summary 만 필수. */
    public static Map<String, Object> responseSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "summary", Map.of("type", "STRING"),
                        "housingGoal", Map.of("type", "STRING", "nullable", true),
                        "discussedConditions", Map.of(
                                "type", "ARRAY",
                                "items", Map.of("type", "STRING")),
                        "actionItems", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "who", Map.of("type", "STRING",
                                                        "enum", List.of("USER", "COUNSELOR")),
                                                "task", Map.of("type", "STRING")),
                                        "required", List.of("who", "task"))),
                        "nextSteps", Map.of("type", "STRING", "nullable", true)),
                "required", List.of("summary"));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank() || "null".equals(s)) ? null : s.trim();
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}

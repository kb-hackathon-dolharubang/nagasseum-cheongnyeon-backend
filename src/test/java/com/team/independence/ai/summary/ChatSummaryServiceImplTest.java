package com.team.independence.ai.summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.independence.ai.client.GeminiClient;
import com.team.independence.ai.client.GeminiException;
import com.team.independence.ai.summary.dto.SummaryReport;
import com.team.independence.ai.summary.dto.SummaryRequest;
import com.team.independence.ai.summary.service.ChatSummaryServiceImpl;
import com.team.independence.ai.summary.service.SummaryPromptBuilder;
import com.team.independence.common.exception.BusinessException;
import com.team.independence.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatSummaryServiceImplTest {

    private GeminiClient gemini;
    private ChatSummaryServiceImpl service;

    @BeforeEach
    void setUp() {
        gemini = mock(GeminiClient.class);
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new ChatSummaryServiceImpl(gemini, new SummaryPromptBuilder(), om);
    }

    private SummaryRequest anyRequest() {
        // 리플렉션 없이도 되도록 Jackson 으로 만든다.
        try {
            ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
            return om.readValue(
                    "{\"messages\":[{\"role\":\"USER\",\"text\":\"안녕하세요\"}]}",
                    SummaryRequest.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("정상 JSON → 리포트 파싱 (느슨한 스키마: summary 필수, 나머지 optional)")
    void parsesReport() {
        when(gemini.generateJson(any(), any(), any())).thenReturn("""
                {
                  "summary": "전세 자금 마련 상담을 진행했습니다.",
                  "housingGoal": "2년 내 전세 보증금 1억 마련",
                  "discussedConditions": ["예산 범위", "희망 지역"],
                  "actionItems": [
                    {"who": "USER", "task": "청약통장 납입 내역 정리"},
                    {"who": "COUNSELOR", "task": "지역 시세 자료 준비"}
                  ],
                  "nextSteps": "다음 상담에서 대출 한도 검토"
                }
                """);

        SummaryReport r = service.summarize(anyRequest());

        assertThat(r.getSummary()).contains("전세 자금");
        assertThat(r.getHousingGoal()).contains("1억");
        assertThat(r.getDiscussedConditions()).containsExactly("예산 범위", "희망 지역");
        assertThat(r.getActionItems()).hasSize(2);
        assertThat(r.getActionItems().get(0).getWho()).isEqualTo("USER");
        assertThat(r.getNextSteps()).contains("대출 한도");
    }

    @Test
    @DisplayName("summary 만 있고 나머지 비어도 OK (억지 채움 방지)")
    void summaryOnly() {
        when(gemini.generateJson(any(), any(), any())).thenReturn(
                "{\"summary\":\"짧은 인사만 오간 대화입니다.\",\"discussedConditions\":[],\"actionItems\":[]}");

        SummaryReport r = service.summarize(anyRequest());

        assertThat(r.getSummary()).isNotBlank();
        assertThat(r.getHousingGoal()).isNull();
        assertThat(r.getDiscussedConditions()).isEmpty();
        assertThat(r.getActionItems()).isEmpty();
        assertThat(r.getNextSteps()).isNull();
    }

    @Test
    @DisplayName("summary 없음 → SUMMARY_AI_FAILED")
    void missingSummaryThrows() {
        when(gemini.generateJson(any(), any(), any())).thenReturn("{\"housingGoal\":\"x\"}");

        assertThatThrownBy(() -> service.summarize(anyRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SUMMARY_AI_FAILED));
    }

    @Test
    @DisplayName("깨진 JSON → SUMMARY_AI_FAILED")
    void brokenJsonThrows() {
        when(gemini.generateJson(any(), any(), any())).thenReturn("이건 JSON 아님");

        assertThatThrownBy(() -> service.summarize(anyRequest()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Gemini 호출 실패 → SUMMARY_AI_FAILED")
    void geminiExceptionMapped() {
        when(gemini.generateJson(any(), any(), any())).thenThrow(new GeminiException("HTTP 503"));

        assertThatThrownBy(() -> service.summarize(anyRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SUMMARY_AI_FAILED));
    }

    @Test
    @DisplayName("responseSchema 는 summary 만 required")
    void responseSchemaShape() {
        Map<String, Object> schema = ChatSummaryServiceImpl.responseSchema();
        assertThat(schema).containsEntry("type", "OBJECT");
        assertThat(schema).containsEntry("required", List.of("summary"));
    }
}

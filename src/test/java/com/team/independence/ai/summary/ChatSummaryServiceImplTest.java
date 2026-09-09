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
    private final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        gemini = mock(GeminiClient.class);
        service = new ChatSummaryServiceImpl(gemini, new SummaryPromptBuilder(), om);
    }

    private SummaryRequest anyRequest() {
        try {
            return om.readValue("""
                    {
                      "reservationId": 12,
                      "consultationType": "GENERAL",
                      "category": "HOUSING",
                      "consultInfo": {
                        "housingPreference": {
                          "province": "서울특별시", "district": "마포구", "neighborhood": "서교동",
                          "housingType": "OFFICETEL", "transactionType": "JEONSE",
                          "areaRange": { "min": 10, "max": 20, "label": "10~20평" }
                        },
                        "currentAsset": 45000000, "monthlySaving": 900000,
                        "targetDate": "2031-08", "loanPreference": "UNDECIDED"
                      },
                      "messages": [
                        { "senderType": "USER", "content": "희망 조건을 유지하고 싶어요.", "createdAt": "2026-09-09T14:01:00" },
                        { "senderType": "COUNSELOR", "content": "목표 시점 조정도 방법입니다.", "createdAt": "2026-09-09T14:02:00" }
                      ]
                    }
                    """, SummaryRequest.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("정상 JSON → 5필드 파싱")
    void parsesReport() {
        when(gemini.generateJson(any(), any(), any())).thenReturn("""
                {
                  "summary": "현재 자산과 희망 조건을 기준으로 목표 달성 가능성을 검토했습니다.",
                  "mainConcerns": ["희망 조건 유지 가능 여부", "목표 시점 유지 가능 여부"],
                  "discussionPoints": ["현재 자산과 필요 금액 비교", "희망 조건 예상 비용"],
                  "result": "월 저축액을 높이거나 목표 시점을 조정할 필요가 있습니다.",
                  "recommendations": ["월 저축액 조정 검토", "목표 시점 조정 검토"]
                }
                """);

        SummaryReport r = service.summarize(anyRequest());

        assertThat(r.getSummary()).contains("목표 달성 가능성");
        assertThat(r.getMainConcerns()).hasSize(2);
        assertThat(r.getDiscussionPoints()).containsExactly("현재 자산과 필요 금액 비교", "희망 조건 예상 비용");
        assertThat(r.getResult()).contains("목표 시점");
        assertThat(r.getRecommendations()).containsExactly("월 저축액 조정 검토", "목표 시점 조정 검토");
    }

    @Test
    @DisplayName("summary·result 만 있고 배열은 비어도 OK (억지 채움 방지)")
    void requiredOnly() {
        when(gemini.generateJson(any(), any(), any())).thenReturn(
                "{\"summary\":\"짧은 상담입니다.\",\"result\":\"추가 논의가 필요합니다.\","
                + "\"mainConcerns\":[],\"discussionPoints\":[],\"recommendations\":[]}");

        SummaryReport r = service.summarize(anyRequest());

        assertThat(r.getSummary()).isNotBlank();
        assertThat(r.getResult()).isNotBlank();
        assertThat(r.getMainConcerns()).isEmpty();
        assertThat(r.getDiscussionPoints()).isEmpty();
        assertThat(r.getRecommendations()).isEmpty();
    }

    @Test
    @DisplayName("배열 필드 자체가 없어도 빈 리스트로")
    void missingArraysBecomeEmpty() {
        when(gemini.generateJson(any(), any(), any())).thenReturn(
                "{\"summary\":\"요약\",\"result\":\"결과\"}");

        SummaryReport r = service.summarize(anyRequest());

        assertThat(r.getMainConcerns()).isEmpty();
        assertThat(r.getDiscussionPoints()).isEmpty();
        assertThat(r.getRecommendations()).isEmpty();
    }

    @Test
    @DisplayName("summary 없음 → SUMMARY_AI_FAILED")
    void missingSummaryThrows() {
        when(gemini.generateJson(any(), any(), any())).thenReturn("{\"result\":\"x\"}");

        assertThatThrownBy(() -> service.summarize(anyRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SUMMARY_AI_FAILED));
    }

    @Test
    @DisplayName("result 없음 → SUMMARY_AI_FAILED")
    void missingResultThrows() {
        when(gemini.generateJson(any(), any(), any())).thenReturn("{\"summary\":\"x\"}");

        assertThatThrownBy(() -> service.summarize(anyRequest()))
                .isInstanceOf(BusinessException.class);
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
    @DisplayName("diagnosis 있는 요청도 파싱된다 (목표 진단 연계)")
    void withDiagnosis() {
        try {
            SummaryRequest req = om.readValue("""
                    {
                      "reservationId": 12, "consultationType": "GENERAL", "category": "HOUSING",
                      "consultInfo": { "currentAsset": 45000000, "monthlySaving": 900000, "targetDate": "2031-08" },
                      "diagnosis": {
                        "originalCondition": { "region": "서울 마포구", "housingType": "오피스텔", "transactionType": "전세", "area": "10~20평" },
                        "recommendedCondition": { "region": "서울 마포구", "housingType": "단독·다가구", "transactionType": "전세", "area": "4~9평" },
                        "targetDate": "2031-08", "recommendedMonthlySaving": 1100000
                      },
                      "messages": [ { "senderType": "USER", "content": "조정안이 궁금해요." } ]
                    }
                    """, SummaryRequest.class);
            when(gemini.generateJson(any(), any(), any())).thenReturn(
                    "{\"summary\":\"진단 결과를 함께 검토했습니다.\",\"result\":\"추천 조건으로 조정 시 목표 시점 유지가 가능합니다.\","
                    + "\"mainConcerns\":[],\"discussionPoints\":[\"원래 조건과 추천 조건 비교\"],\"recommendations\":[\"추천 조건 검토\"]}");

            SummaryReport r = service.summarize(req);
            assertThat(r.getDiscussionPoints()).contains("원래 조건과 추천 조건 비교");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("responseSchema 는 summary·result 만 required")
    void responseSchemaShape() {
        Map<String, Object> schema = ChatSummaryServiceImpl.responseSchema();
        assertThat(schema).containsEntry("type", "OBJECT");
        assertThat(schema).containsEntry("required", List.of("summary", "result"));
    }
}

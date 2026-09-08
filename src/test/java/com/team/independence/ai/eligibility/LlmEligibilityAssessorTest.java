package com.team.independence.ai.eligibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.independence.ai.client.GeminiClient;
import com.team.independence.ai.client.GeminiException;
import com.team.independence.ai.eligibility.model.HouseholdRole;
import com.team.independence.ai.eligibility.model.MaritalStatus;
import com.team.independence.ai.eligibility.model.UserProfile;
import com.team.independence.ai.eligibility.policy.BeotimmokJeonseLoanPolicy;
import com.team.independence.ai.eligibility.service.EligibilityPromptBuilder;
import com.team.independence.ai.eligibility.service.LlmEligibilityAssessor;
import com.team.independence.common.exception.BusinessException;
import com.team.independence.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmEligibilityAssessorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 8);

    private GeminiClient gemini;
    private LlmEligibilityAssessor assessor;

    private final BeotimmokJeonseLoanPolicy policy = new BeotimmokJeonseLoanPolicy();

    private final UserProfile profile = UserProfile.builder()
            .birthDate(LocalDate.of(1993, 4, 1))
            .combinedAnnualIncome(40_000_000L)
            .allHouseholdMembersHouseless(true)
            .householdRole(HouseholdRole.HEAD)
            .maritalStatus(MaritalStatus.SINGLE)
            .build();

    @BeforeEach
    void setUp() {
        gemini = mock(GeminiClient.class);
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        assessor = new LlmEligibilityAssessor(gemini, new EligibilityPromptBuilder(), om);
    }

    @Test
    @DisplayName("정상 JSON → advice 파싱")
    void parsesAdvice() {
        when(gemini.generateJson(any(), any(), any()))
                .thenReturn("{\"advice\":\"순자산 정보를 입력하시면 자산 요건까지 확인할 수 있습니다.\"}");

        String advice = assessor.generateAdvice(policy, profile, List.of(), AS_OF);

        assertThat(advice).isEqualTo("순자산 정보를 입력하시면 자산 요건까지 확인할 수 있습니다.");
    }

    @Test
    @DisplayName("advice 필드 없음 → POLICY_AI_ASSESSMENT_FAILED")
    void missingAdviceThrows() {
        when(gemini.generateJson(any(), any(), any())).thenReturn("{\"foo\":\"bar\"}");

        assertThatThrownBy(() -> assessor.generateAdvice(policy, profile, List.of(), AS_OF))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.POLICY_AI_ASSESSMENT_FAILED));
    }

    @Test
    @DisplayName("깨진 JSON → POLICY_AI_ASSESSMENT_FAILED")
    void brokenJsonThrows() {
        when(gemini.generateJson(any(), any(), any())).thenReturn("이건 JSON 이 아님");

        assertThatThrownBy(() -> assessor.generateAdvice(policy, profile, List.of(), AS_OF))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Gemini 호출 자체 실패(GeminiException) → POLICY_AI_ASSESSMENT_FAILED")
    void geminiExceptionMapped() {
        when(gemini.generateJson(any(), any(), any())).thenThrow(new GeminiException("HTTP 503"));

        assertThatThrownBy(() -> assessor.generateAdvice(policy, profile, List.of(), AS_OF))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.POLICY_AI_ASSESSMENT_FAILED));
    }

    @Test
    @DisplayName("responseSchema 는 advice 하나짜리 OBJECT")
    void responseSchemaShape() {
        Map<String, Object> schema = LlmEligibilityAssessor.responseSchema();
        assertThat(schema).containsEntry("type", "OBJECT");
        assertThat(schema).containsKey("properties");
        assertThat(schema).containsEntry("required", List.of("advice"));
    }
}

package com.team.independence.ai.eligibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.independence.ai.client.GeminiClient;
import com.team.independence.ai.client.GeminiProperties;
import com.team.independence.ai.client.GeminiRestClient;
import com.team.independence.ai.eligibility.dto.EligibilityRequest;
import com.team.independence.ai.eligibility.dto.EligibilityResult;
import com.team.independence.ai.eligibility.model.HouseholdRole;
import com.team.independence.ai.eligibility.model.MaritalStatus;
import com.team.independence.ai.eligibility.policy.BeotimmokJeonseLoanPolicy;
import com.team.independence.ai.eligibility.policy.PolicyRegistry;
import com.team.independence.ai.eligibility.service.EligibilityPromptBuilder;
import com.team.independence.ai.eligibility.service.LlmEligibilityAssessor;
import com.team.independence.ai.eligibility.service.PolicyEligibilityService;
import com.team.independence.ai.eligibility.service.PolicyEligibilityServiceImpl;
import com.team.independence.ai.eligibility.service.RuleEvaluator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Gemini 를 <b>딱 1번</b> 호출해 조언(advice) 생성이 도는지 눈으로 확인하는 수동 테스트.
 * 무료 티어 일일 한도(20/day)를 아끼기 위해 시나리오 1건만 호출한다.
 *
 * <p>실행:
 * <pre>
 * JAVA_HOME=... mvn test -Dtest=PolicyEligibilityLiveManualTest \
 *   -Djunit.jupiter.conditions.deactivate='org.junit.jupiter.engine.extension.DisabledCondition'
 * </pre>
 */
@Disabled("실제 Gemini 호출 - 로컬에서 수동 실행")
class PolicyEligibilityLiveManualTest {

    @Test
    @DisplayName("버팀목 1건 실제 호출 (소득 초과+신혼+순자산 미입력 → advice 가 예외상한·미입력·신용도 안내하는지)")
    void singleLiveCall() throws Exception {
        Map<String, String> env = readDotEnv();
        String apiKey = env.getOrDefault("GEMINI_API_KEY", "").trim();
        String model = env.getOrDefault("GEMINI_MODEL", "gemini-3.6-flash").trim();
        org.junit.jupiter.api.Assumptions.assumeTrue(!apiKey.isBlank(), ".env 에 GEMINI_API_KEY 없음");

        GeminiProperties props = new GeminiProperties() {
            @Override public String getApiKey() { return apiKey; }
            @Override public String getModel() { return model; }
            @Override public String getBaseUrl() { return "https://generativelanguage.googleapis.com/v1beta"; }
            @Override public int getTimeoutMs() { return 30_000; }
            @Override public int getMaxRetries() { return 5; }
            @Override public double getTemperature() { return 0.2; }
        };

        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        ObjectMapper pretty = om.copy().enable(SerializationFeature.INDENT_OUTPUT);

        GeminiClient client = new GeminiRestClient(props, om);
        var assessor = new LlmEligibilityAssessor(client, new EligibilityPromptBuilder(), om);
        var registry = new PolicyRegistry(List.of(new BeotimmokJeonseLoanPolicy()));
        PolicyEligibilityService service =
                new PolicyEligibilityServiceImpl(registry, new RuleEvaluator(), assessor);

        EligibilityRequest req = new EligibilityRequest();
        set(req, "policyId", BeotimmokJeonseLoanPolicy.ID);
        // 필수 5
        set(req, "birthDate", LocalDate.of(1992, 6, 15));       // 만 34세
        set(req, "combinedAnnualIncome", 60_000_000L);          // 기본 5천만 초과 → 소득 FAIL
        set(req, "allHouseholdMembersHouseless", true);
        set(req, "householdRole", HouseholdRole.HEAD);
        set(req, "maritalStatus", MaritalStatus.MARRIED);       // 신혼 예외 상한 가능성
        // 선택
        set(req, "marriageDate", LocalDate.of(2025, 3, 1));
        // combinedNetAsset 미입력 / usingFundLoan 미입력 / 공공임대 미입력 → advice 가 안내해야 함

        System.out.println("\n================ 버팀목 · 실제 Gemini(" + model + ") 조언 생성 (1회) ================\n");
        EligibilityResult res = service.assess(req);
        System.out.println("--- INPUT ---");
        System.out.println(pretty.writeValueAsString(req));
        System.out.println("--- OUTPUT ---");
        System.out.println(pretty.writeValueAsString(res));
        System.out.println("\n================ 완료 ================\n");

        assertThat(res.getCoreFindings()).hasSize(4);
        assertThat(res.getAdvice()).isNotBlank();
    }

    private static Map<String, String> readDotEnv() throws IOException {
        Map<String, String> m = new LinkedHashMap<>();
        Path p = Path.of(System.getProperty("user.dir"), ".env");
        if (!Files.exists(p)) return m;
        for (String line : Files.readAllLines(p)) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#") || !s.contains("=")) continue;
            int i = s.indexOf('=');
            m.put(s.substring(0, i).trim(), s.substring(i + 1).trim());
        }
        return m;
    }

    /** EligibilityRequest 는 세터가 없어(요청 DTO) 테스트에서 리플렉션으로 채운다. */
    private static void set(EligibilityRequest r, String field, Object value) {
        try {
            var f = EligibilityRequest.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(r, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("필드 주입 실패: " + field, e);
        }
    }
}

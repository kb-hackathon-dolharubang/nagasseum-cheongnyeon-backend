package com.team.independence.ai.summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.independence.ai.client.GeminiClient;
import com.team.independence.ai.client.GeminiProperties;
import com.team.independence.ai.client.GeminiRestClient;
import com.team.independence.ai.summary.dto.SummaryReport;
import com.team.independence.ai.summary.dto.SummaryRequest;
import com.team.independence.ai.summary.service.ChatSummaryServiceImpl;
import com.team.independence.ai.summary.service.SummaryPromptBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Gemini 를 <b>딱 1번</b> 호출해 상담 요약이 도는지 눈으로 확인하는 수동 테스트.
 * 무료 티어 일일 한도(20/day)를 아끼기 위해 1건만 호출한다.
 *
 * <p>실행:
 * <pre>
 * JAVA_HOME=... mvn test -Dtest=ChatSummaryLiveManualTest \
 *   -Djunit.jupiter.conditions.deactivate='org.junit.jupiter.engine.extension.DisabledCondition'
 * </pre>
 */
@Disabled("실제 Gemini 호출 - 로컬에서 수동 실행")
class ChatSummaryLiveManualTest {

    @Test
    @DisplayName("목표 진단 연계 상담 1건 실제 요약")
    void singleLiveCall() throws Exception {
        Map<String, String> env = readDotEnv();
        String apiKey = env.getOrDefault("GEMINI_API_KEY", "").trim();
        String model = env.getOrDefault("GEMINI_MODEL", "gemini-3.6-flash").trim();
        org.junit.jupiter.api.Assumptions.assumeTrue(!apiKey.isBlank(), ".env 에 GEMINI_API_KEY 없음");

        GeminiProperties props = new GeminiProperties() {
            @Override public String getApiKey() { return apiKey; }
            @Override public String getModel() { return model; }
            @Override public String getBaseUrl() { return "https://generativelanguage.googleapis.com/v1beta"; }
            @Override public int getTimeoutMs() { return 60_000; }
            @Override public int getMaxRetries() { return 5; }
            @Override public double getTemperature() { return 0.2; }
        };

        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        ObjectMapper pretty = om.copy().enable(SerializationFeature.INDENT_OUTPUT);

        GeminiClient client = new GeminiRestClient(props, om);
        ChatSummaryServiceImpl service =
                new ChatSummaryServiceImpl(client, new SummaryPromptBuilder(), om);

        String reqJson = """
                {
                  "reservationId": 12,
                  "consultationType": "GENERAL",
                  "category": "HOUSING",
                  "preConsultationQuestion": "현재 자산으로 희망 조건을 유지할 수 있는지 궁금합니다.",
                  "consultInfo": {
                    "housingPreference": {
                      "province": "서울특별시", "district": "마포구", "neighborhood": "서교동",
                      "housingType": "OFFICETEL", "transactionType": "JEONSE",
                      "areaRange": { "min": 10, "max": 20, "label": "10~20평" }
                    },
                    "currentAsset": 45000000,
                    "monthlySaving": 900000,
                    "targetDate": "2031-08",
                    "loanPreference": "UNDECIDED"
                  },
                  "diagnosis": {
                    "originalCondition": { "region": "서울 마포구", "housingType": "오피스텔", "transactionType": "전세", "area": "10~20평" },
                    "recommendedCondition": { "region": "서울 마포구", "housingType": "단독·다가구", "transactionType": "전세", "area": "4~9평" },
                    "targetDate": "2031-08",
                    "recommendedMonthlySaving": 1100000
                  },
                  "messages": [
                    { "senderType": "USER", "content": "현재 희망 조건을 그대로 유지하고 싶어요.", "createdAt": "2026-09-09T14:01:00" },
                    { "senderType": "COUNSELOR", "content": "현재 자산과 월 저축 가능액을 기준으로 보면 목표 시점을 조금 조정하는 방법도 있습니다.", "createdAt": "2026-09-09T14:02:00" },
                    { "senderType": "USER", "content": "저축액을 더 늘리는 건 부담이라, 조건을 조금 낮추는 쪽이 나을까요?", "createdAt": "2026-09-09T14:03:00" },
                    { "senderType": "COUNSELOR", "content": "네, 추천 조건처럼 면적을 낮추면 목표 시점을 유지하면서 필요 자금이 줄어듭니다. 대출 활용 여부도 함께 보면 좋습니다.", "createdAt": "2026-09-09T14:04:00" }
                  ]
                }
                """;
        SummaryRequest req = om.readValue(reqJson, SummaryRequest.class);

        System.out.println("\n================ 상담 요약 · 실제 Gemini(" + model + ") (1회) ================\n");
        System.out.println("--- INPUT ---");
        System.out.println(pretty.writeValueAsString(req));
        SummaryReport res = service.summarize(req);
        System.out.println("--- OUTPUT ---");
        System.out.println(pretty.writeValueAsString(res));
        System.out.println("\n================ 완료 ================\n");

        assertThat(res.getSummary()).isNotBlank();
        assertThat(res.getResult()).isNotBlank();
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
}

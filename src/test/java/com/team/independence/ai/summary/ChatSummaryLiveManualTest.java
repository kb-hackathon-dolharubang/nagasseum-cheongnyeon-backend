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
 * 실제 Gemini 를 <b>딱 1번</b> 호출해 채팅 요약이 도는지 눈으로 확인하는 수동 테스트.
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
    @DisplayName("주거 상담 대화 1건 실제 요약")
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
        ChatSummaryServiceImpl service =
                new ChatSummaryServiceImpl(client, new SummaryPromptBuilder(), om);

        String reqJson = """
                {
                  "roomTitle": "전세 자금 마련 상담",
                  "messages": [
                    {"role":"COUNSELOR","text":"안녕하세요, 오늘은 어떤 주거 목표를 상담받고 싶으신가요?","sentAt":"2026-09-08T14:00:00"},
                    {"role":"USER","text":"2년 안에 서울 근처에 전세로 독립하고 싶어요. 지금 보증금이 부족해서요.","sentAt":"2026-09-08T14:01:10"},
                    {"role":"COUNSELOR","text":"희망하시는 보증금 수준과 지역이 어떻게 되실까요?","sentAt":"2026-09-08T14:01:40"},
                    {"role":"USER","text":"1억 정도 생각하고 있고, 경기도 성남이나 하남 쪽이면 좋겠어요.","sentAt":"2026-09-08T14:02:20"},
                    {"role":"COUNSELOR","text":"현재 매달 저축 가능한 금액은 어느 정도세요?","sentAt":"2026-09-08T14:02:50"},
                    {"role":"USER","text":"월 80만원 정도는 꾸준히 넣을 수 있어요. 지금 모아둔 건 3천만원쯤 됩니다.","sentAt":"2026-09-08T14:03:30"},
                    {"role":"COUNSELOR","text":"그러면 버팀목 전세자금대출 같은 정책 대출을 함께 활용하는 걸 검토해보면 좋겠습니다. 다음 상담 때 소득 요건을 같이 확인해볼게요.","sentAt":"2026-09-08T14:04:30"},
                    {"role":"USER","text":"네 좋아요. 청약통장도 있는데 그것도 활용할 수 있을까요?","sentAt":"2026-09-08T14:05:00"},
                    {"role":"COUNSELOR","text":"네, 청약통장 납입 내역을 다음에 가져와 주시면 같이 살펴보겠습니다.","sentAt":"2026-09-08T14:05:30"}
                  ]
                }
                """;
        SummaryRequest req = om.readValue(reqJson, SummaryRequest.class);

        System.out.println("\n================ 채팅 요약 · 실제 Gemini(" + model + ") (1회) ================\n");
        System.out.println("--- INPUT ---");
        System.out.println(pretty.writeValueAsString(req));
        SummaryReport res = service.summarize(req);
        System.out.println("--- OUTPUT ---");
        System.out.println(pretty.writeValueAsString(res));
        System.out.println("\n================ 완료 ================\n");

        assertThat(res.getSummary()).isNotBlank();
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

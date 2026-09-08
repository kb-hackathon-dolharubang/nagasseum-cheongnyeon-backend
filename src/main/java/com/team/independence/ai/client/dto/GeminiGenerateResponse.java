package com.team.independence.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * generateContent 응답 껍데기. 필요한 경로({@code candidates[0].content.parts[0].text})만 매핑한다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiGenerateResponse {

    private List<Candidate> candidates;
    private PromptFeedback promptFeedback;

    /** 후보 text 를 뽑아낸다. 없으면 null. */
    public String firstText() {
        if (candidates == null || candidates.isEmpty()) return null;
        Candidate c = candidates.get(0);
        if (c.content == null || c.content.parts == null || c.content.parts.isEmpty()) return null;
        return c.content.parts.get(0).text;
    }

    public String firstFinishReason() {
        return (candidates == null || candidates.isEmpty()) ? null : candidates.get(0).finishReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
        private String finishReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private List<Part> parts;
        private String role;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        private String text;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromptFeedback {
        private String blockReason;
    }
}

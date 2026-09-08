package com.team.independence.ai.summary.service;

import com.team.independence.ai.summary.dto.SummaryRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * 채팅 요약 프롬프트 조립기. 문구(템플릿)는 {@code resources/prompt/chat-summary.txt} 로 분리한다.
 */
@Component
public class SummaryPromptBuilder {

    static final String SYSTEM_INSTRUCTION =
            "너는 청년 주거 상담 내용을 사용자에게 줄 리포트로 요약하는 보조자다. "
            + "대화에 실제로 나온 내용만 사용하고, 없는 사실을 추측하거나 지어내지 마라. "
            + "실명·연락처 등 개인식별정보는 요약에 담지 마라. "
            + "반드시 제공된 JSON 스키마 형식으로만 답하라.";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final String template;

    public SummaryPromptBuilder() {
        this.template = load("prompt/chat-summary.txt");
    }

    public String build(SummaryRequest request) {
        String roomTitle = (request.getRoomTitle() == null || request.getRoomTitle().isBlank())
                ? "(없음)" : request.getRoomTitle().trim();
        return template
                .replace("{{roomTitle}}", roomTitle)
                .replace("{{conversation}}", renderConversation(request));
    }

    private String renderConversation(SummaryRequest request) {
        StringBuilder sb = new StringBuilder();
        for (SummaryRequest.Message m : request.getMessages()) {
            String who = m.getRole() == com.team.independence.ai.summary.model.MessageRole.USER ? "사용자" : "상담사";
            sb.append('[');
            if (m.getSentAt() != null) sb.append(m.getSentAt().format(TS)).append(' ');
            sb.append(who).append("] ").append(m.getText().strip()).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static String load(String path) {
        try {
            byte[] bytes = new ClassPathResource(path).getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("프롬프트 템플릿 로드 실패: " + path, e);
        }
    }
}

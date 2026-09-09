package com.team.independence.ai.summary.service;

import com.team.independence.ai.summary.dto.SummaryRequest;
import com.team.independence.ai.summary.model.SenderType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 상담 요약 프롬프트 조립기. 문구(템플릿)는 {@code resources/prompt/chat-summary.txt} 로 분리한다.
 * 프론트가 준 값(코드값·한글 문자열)을 정규화하지 않고 그대로 렌더한다.
 */
@Component
public class SummaryPromptBuilder {

    static final String SYSTEM_INSTRUCTION =
            "너는 청년 주거 상담 내용을 사용자에게 줄 리포트로 요약하는 보조자다. "
            + "대화와 제공된 정보에 실제로 나온 내용만 사용하고, 없는 사실을 추측하거나 지어내지 마라. "
            + "실명·연락처 등 개인식별정보는 요약에 담지 마라. "
            + "반드시 제공된 JSON 스키마 형식으로만 답하라.";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final String template;

    public SummaryPromptBuilder() {
        this.template = load("prompt/chat-summary.txt");
    }

    public String build(SummaryRequest req) {
        return template
                .replace("{{consultationType}}", nvl(req.getConsultationType()))
                .replace("{{category}}", nvl(req.getCategory()))
                .replace("{{preConsultationQuestion}}",
                        blank(req.getPreConsultationQuestion()) ? "(없음)" : req.getPreConsultationQuestion().trim())
                .replace("{{consultInfo}}", renderConsultInfo(req.getConsultInfo()))
                .replace("{{diagnosisBlock}}", renderDiagnosis(req.getDiagnosis()))
                .replace("{{conversation}}", renderConversation(req));
    }

    private String renderConsultInfo(SummaryRequest.ConsultInfo c) {
        if (c == null) return "(제공되지 않음)";
        StringBuilder sb = new StringBuilder();
        SummaryRequest.HousingPreference h = c.getHousingPreference();
        if (h != null) {
            String region = join(" ", h.getProvince(), h.getDistrict(), h.getNeighborhood());
            line(sb, "희망 지역", blank(region) ? "미입력" : region);
            line(sb, "희망 주택유형", nvl(h.getHousingType()));
            line(sb, "희망 거래유형", nvl(h.getTransactionType()));
            SummaryRequest.AreaRange a = h.getAreaRange();
            if (a != null) {
                String label = blank(a.getLabel())
                        ? (a.getMin() + "~" + a.getMax()) : a.getLabel();
                line(sb, "희망 면적", label);
            }
        }
        if (c.getCurrentAsset() != null) line(sb, "현재 자산", won(c.getCurrentAsset()));
        if (c.getMonthlySaving() != null) line(sb, "월 저축 가능액", won(c.getMonthlySaving()));
        if (!blank(c.getTargetDate())) line(sb, "목표 시점", c.getTargetDate().trim());
        if (!blank(c.getLoanPreference())) line(sb, "대출 활용 의향", c.getLoanPreference().trim());
        return sb.length() == 0 ? "(제공되지 않음)" : sb.toString().stripTrailing();
    }

    private String renderDiagnosis(SummaryRequest.Diagnosis d) {
        if (d == null) return "(제공되지 않음)";
        StringBuilder sb = new StringBuilder();
        line(sb, "원래 조건", renderCondition(d.getOriginalCondition()));
        line(sb, "추천 조건", renderCondition(d.getRecommendedCondition()));
        if (!blank(d.getTargetDate())) line(sb, "목표 시점", d.getTargetDate().trim());
        if (d.getRecommendedMonthlySaving() != null) {
            line(sb, "추천 월 저축액", won(d.getRecommendedMonthlySaving()));
        }
        return sb.toString().stripTrailing();
    }

    private String renderCondition(SummaryRequest.DiagnosisCondition c) {
        if (c == null) return "미상";
        return join(" / ", c.getRegion(), c.getHousingType(), c.getTransactionType(), c.getArea());
    }

    private String renderConversation(SummaryRequest req) {
        StringBuilder sb = new StringBuilder();
        for (SummaryRequest.Message m : req.getMessages()) {
            String who = m.getSenderType() == SenderType.USER ? "사용자" : "상담사";
            sb.append('[');
            if (m.getCreatedAt() != null) sb.append(m.getCreatedAt().format(TS)).append(' ');
            sb.append(who).append("] ").append(m.getContent().strip()).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static void line(StringBuilder sb, String k, String v) {
        sb.append("- ").append(k).append(": ").append(v).append('\n');
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String nvl(String s) {
        return blank(s) ? "미입력" : s.trim();
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (blank(p)) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p.trim());
        }
        return sb.toString();
    }

    private static String won(long v) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(v) + "원";
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

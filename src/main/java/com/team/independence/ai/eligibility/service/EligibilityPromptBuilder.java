package com.team.independence.ai.eligibility.service;

import com.team.independence.ai.eligibility.model.RuleFinding;
import com.team.independence.ai.eligibility.model.UserProfile;
import com.team.independence.ai.eligibility.policy.PolicyDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * 조언(advice) 생성 프롬프트 조립기. 문구(템플릿)는 {@code resources/prompt/policy-eligibility.txt} 로
 * 분리해 코드 수정 없이 깎을 수 있게 한다. 여기서는 자리표시자만 채운다.
 */
@Component
public class EligibilityPromptBuilder {

    static final String SYSTEM_INSTRUCTION =
            "너는 대한민국 주택 정책 신청인에게 조언을 주는 보조자다. "
            + "적격/부적격 판정은 코드가 이미 끝냈고, 너는 판정하지 않는다. 조언 문구만 작성한다. "
            + "주어진 신청인 정보에 없는 사실을 추측하거나 지어내지 마라. 모르면 '확인이 필요하다'로 쓴다. "
            + "최종 적격/부적격을 단정하거나 확률·점수 같은 숫자를 만들지 마라. "
            + "반드시 제공된 JSON 스키마({ \"advice\": \"...\" }) 형식으로만 답하라.";

    private final String template;

    public EligibilityPromptBuilder() {
        this.template = load("prompt/policy-eligibility.txt");
    }

    public String build(PolicyDefinition policy, UserProfile p, List<RuleFinding> coreFindings, LocalDate asOf) {
        return template
                .replace("{{policyName}}", policy.name())
                .replace("{{userProfile}}", renderProfile(p, asOf))
                .replace("{{coreFindings}}", renderCoreFindings(coreFindings))
                .replace("{{adviceContext}}", policy.adviceContext());
    }

    private String renderProfile(UserProfile p, LocalDate asOf) {
        StringBuilder sb = new StringBuilder();
        // 필수
        line(sb, "생년월일", p.getBirthDate() + " (만 " + p.ageAsOf(asOf) + "세)");
        line(sb, "부부합산 연소득", won(p.getCombinedAnnualIncome()));
        line(sb, "세대원 전원 무주택", yesNo(p.getAllHouseholdMembersHouseless()));
        line(sb, "세대 구분", korRole(p.getHouseholdRole()));
        line(sb, "혼인 상태", korMarital(p.getMaritalStatus()));
        // 선택
        line(sb, "부부합산 순자산", p.getCombinedNetAsset() == null ? "미입력" : won(p.getCombinedNetAsset()));
        line(sb, "혼인신고일/결혼예정일", p.getMarriageDate() == null ? "미입력" : p.getMarriageDate().toString());
        line(sb, "자녀 수", p.getNumberOfChildren() == null ? "미입력" : p.getNumberOfChildren() + "명");
        line(sb, "공공임대주택 입주 중", p.getLivingInPublicRentalHousing() == null ? "미입력"
                : yesNo(p.getLivingInPublicRentalHousing()));
        line(sb, "주택도시기금 대출 이용 중", p.getUsingFundLoan() == null ? "미입력"
                : yesNo(p.getUsingFundLoan()));
        line(sb, "전세자금/주택담보대출 이용 중", p.getUsingJeonseOrMortgageLoan() == null ? "미입력"
                : yesNo(p.getUsingJeonseOrMortgageLoan()));
        return sb.toString().stripTrailing();
    }

    private String renderCoreFindings(List<RuleFinding> findings) {
        StringBuilder sb = new StringBuilder();
        for (RuleFinding f : findings) {
            sb.append("- ").append(f.getRequirement()).append(": ").append(f.getResult().name());
            if (f.getBasis() != null && !f.getBasis().isBlank()) {
                sb.append(" — ").append(f.getBasis());
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static void line(StringBuilder sb, String k, String v) {
        sb.append("- ").append(k).append(": ").append(v).append('\n');
    }

    private static String yesNo(Boolean b) {
        if (b == null) return "미입력";
        return b ? "예" : "아니오";
    }

    private static String korRole(com.team.independence.ai.eligibility.model.HouseholdRole role) {
        switch (role) {
            case HEAD: return "세대주 본인";
            case SPOUSE_OF_HEAD: return "세대주의 배우자";
            case PROSPECTIVE_HEAD: return "예비 세대주";
            default: return "세대원";
        }
    }

    private static String korMarital(com.team.independence.ai.eligibility.model.MaritalStatus s) {
        switch (s) {
            case SINGLE: return "미혼";
            case MARRIED: return "기혼";
            case PROSPECTIVE_MARRIAGE: return "결혼 예정";
            default: return s.name();
        }
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

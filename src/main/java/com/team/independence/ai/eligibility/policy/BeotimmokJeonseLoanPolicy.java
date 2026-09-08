package com.team.independence.ai.eligibility.policy;

import com.team.independence.ai.eligibility.model.HouseholdRole;
import com.team.independence.ai.eligibility.model.RuleFinding;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * 버팀목 전세자금대출 — 대출 대상 요건 (출처: 마이홈포털 "신청대상/대출대상").
 * 계약·대상주택 요건은 이 서비스가 매물을 다루지 않으므로 제외하고, 신청인(사람) 속성만 심사한다.
 *
 * <p>구조: 필수 입력값으로 떨어지는 4개만 <b>핵심 요건(coreRequirements)</b> 으로 코드가 PASS/FAIL 판정한다.
 * 나머지 요건·예외조항(순자산·중복대출·공공임대·신용도·소득 예외상한·예비세대주 등)은 판정하지 않고
 * {@link #adviceContext()} 로 넘겨 LLM 이 조언(advice)으로 안내한다.
 *
 * <p>상수 근거
 * <ul>
 *   <li>{@code ADULT_AGE = 19} — 민법 제4조(성년). "대출접수일 현재 민법상 성년인 세대주"</li>
 *   <li>{@code INCOME_BASE_LIMIT = 5천만원} — 마이홈포털 (소득) 기본 상한. 예외 상한은 adviceContext 로.</li>
 * </ul>
 */
@Component
public class BeotimmokJeonseLoanPolicy implements PolicyDefinition {

    public static final String ID = "beotimmok-jeonse";

    private static final int ADULT_AGE = 19;
    private static final long INCOME_BASE_LIMIT = 50_000_000L;

    // 핵심 요건 라벨 — 규칙평가기 결과 / 출력 coreFindings 에서 요건을 식별하는 키
    public static final String R_ADULT_HEAD  = "성년(민법상 성년)";
    public static final String R_HEAD_STATUS = "세대주 지위";
    public static final String R_HOUSELESS   = "세대원 전원 무주택";
    public static final String R_INCOME_BASE = "소득(기본 5천만원 이하)";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "버팀목 전세자금대출";
    }

    @Override
    public List<CoreRequirement> coreRequirements() {
        return List.of(
                CoreRequirement.of(R_ADULT_HEAD, (p, asOf) -> {
                    int age = p.ageAsOf(asOf);
                    return age >= ADULT_AGE
                            ? RuleFinding.pass(R_ADULT_HEAD, "만 " + age + "세")
                            : RuleFinding.fail(R_ADULT_HEAD, "만 " + age + "세 (민법상 성년 미만)");
                }),

                CoreRequirement.of(R_HEAD_STATUS, (p, asOf) -> {
                    HouseholdRole role = p.getHouseholdRole();
                    if (role == HouseholdRole.MEMBER) {
                        return RuleFinding.fail(R_HEAD_STATUS, "세대원 (세대주 아님)");
                    }
                    return RuleFinding.pass(R_HEAD_STATUS, korRole(role));
                }),

                CoreRequirement.of(R_HOUSELESS, (p, asOf) ->
                        p.getAllHouseholdMembersHouseless()
                                ? RuleFinding.pass(R_HOUSELESS, "세대원 전원 무주택")
                                : RuleFinding.fail(R_HOUSELESS, "무주택 아닌 세대원 있음")),

                CoreRequirement.of(R_INCOME_BASE, (p, asOf) -> {
                    long inc = p.getCombinedAnnualIncome();
                    return inc <= INCOME_BASE_LIMIT
                            ? RuleFinding.pass(R_INCOME_BASE, won(inc))
                            : RuleFinding.fail(R_INCOME_BASE,
                                    won(inc) + " (기본 상한 " + won(INCOME_BASE_LIMIT) + " 초과)");
                })
        );
    }

    @Override
    public String adviceContext() {
        return String.join("\n",
                "[소득 예외 상한] 소득 기본 상한은 부부합산 5천만원이나, 다음은 예외 상한을 적용한다:",
                "  · 신혼부부: 7.5천만원 이하",
                "  · 2자녀 가구 / 다자녀 가구 / 혁신도시 이전 공공기관 종사자 / 재개발 구역 세입자 /",
                "    위험건축물 이주지원 대상자: 6천만원 이하",
                "[예비 세대주] 세대주 요건에는 예비 세대주도 포함된다: 대출실행일로부터 1개월 이내 세대분가/합가로",
                "  세대주가 될 예정인 자, 또는 대출접수일 현재 3개월 이내 결혼으로 세대주가 될 예정인 자.",
                "[순자산] 부부합산 순자산 3.45억원(2026년 기준) 이하여야 한다.",
                "[중복대출 금지] 주택도시기금대출 및 은행지원 주택담보대출(전세자금대출 포함) 이용 중이면 원칙적으로 불가.",
                "  단, 한국주택금융공사 주택보증서 담보 기금 임차중도금(잔금 포함) 대출과의 중복은 예외적으로 허용될 수 있다.",
                "[공공임대주택] 대출접수일 현재 공공임대주택에 입주 중이면 원칙적으로 불가.",
                "  단, 대출신청 물건지가 그 목적물이거나 신청인 및 배우자가 그 주택에서 퇴거하는 경우에는 가능하다.",
                "[신용도] 연체·대위변제·부도·금융질서문란 등 신용정보는 이 서비스가 확인할 수 없으며, 취급 은행에서 확인해야 한다."
        );
    }

    private static String korRole(HouseholdRole role) {
        switch (role) {
            case HEAD: return "세대주 본인";
            case SPOUSE_OF_HEAD: return "세대주의 배우자 (세대주 간주)";
            case PROSPECTIVE_HEAD: return "예비 세대주";
            default: return role.name();
        }
    }

    private static String won(long v) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(v) + "원";
    }
}

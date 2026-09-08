package com.team.independence.ai.eligibility.policy;

import com.team.independence.ai.eligibility.model.HouseholdRole;
import com.team.independence.ai.eligibility.model.MaritalStatus;
import com.team.independence.ai.eligibility.model.RuleFinding;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * 내집마련 디딤돌 대출 — 대출 대상 요건 (출처: 마이홈포털 "신청대상/대출대상").
 * 계약·대상주택 요건은 이 서비스가 매물을 다루지 않으므로 제외하고, 신청인(사람) 속성만 심사한다.
 *
 * <p>구조: 필수 입력값(생년월일·소득·무주택·세대주 지위)으로 떨어지는 4개만
 * <b>핵심 요건(coreRequirements)</b> 으로 코드가 PASS/FAIL 판정한다. 나머지 요건·예외조항
 * (순자산·중복대출·신용도·소득 예외상한·만 30세 미만 세대주 특례 등)은 판정하지 않고
 * {@link #adviceContext()} 로 넘겨 LLM 이 조언(advice)으로 안내한다.
 *
 * <p>버팀목과 다른 핵심 상수: 소득 기본 상한 5천만원 → <b>6천만원</b>, 순자산 상한 3.45억(소득3분위) → 5.11억(소득4분위).
 *
 * <p>상수 근거
 * <ul>
 *   <li>{@code ADULT_AGE = 19} — 민법 제4조(성년). "대출접수일 현재 민법상 성년인 세대주"</li>
 *   <li>{@code INCOME_BASE_LIMIT = 6천만원} — 마이홈포털 (소득) 기본 상한. 예외 상한은 adviceContext 로.</li>
 * </ul>
 */
@Component
public class DidimdolLoanPolicy implements PolicyDefinition {

    public static final String ID = "didimdol";

    private static final int ADULT_AGE = 19;
    private static final int YOUNG_SINGLE_HEAD_AGE = 30;
    private static final long INCOME_BASE_LIMIT = 60_000_000L;

    // 핵심 요건 라벨 — 규칙평가기 결과 / 출력 coreFindings 에서 요건을 식별하는 키
    public static final String R_ADULT_HEAD  = "성년(민법상 성년)";
    public static final String R_HEAD_STATUS = "세대주 지위";
    public static final String R_HOUSELESS   = "세대원 전원 무주택";
    public static final String R_INCOME_BASE = "소득(기본 6천만원 이하)";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "내집마련 디딤돌 대출";
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
                    // 만 30세 미만 미혼 세대주는 원칙적으로 대출 제외(단독세대주·미혼세대주 특례).
                    // 단, 직계존·비속과 6개월 이상 동거·부양 시 가능한데, 그 사실은 필수 필드로 알 수 없다.
                    int age = p.ageAsOf(asOf);
                    if (age < YOUNG_SINGLE_HEAD_AGE && p.getMaritalStatus() == MaritalStatus.SINGLE) {
                        return RuleFinding.unknown(R_HEAD_STATUS,
                                "만 " + age + "세 미혼 세대주 — 단독세대주는 대출 제외 대상이나, "
                                + "직계존·비속(또는 미성년 형제·자매)과 6개월 이상 동거·부양 중이면 가능. 해당 여부 확인 필요");
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
                "[소득 예외 상한] 소득 기본 상한은 부부합산 연 6천만원이나, 다음은 예외 상한을 적용한다:",
                "  · 신혼가구: 연 8.5천만원 이하",
                "  · 생애최초 주택구입자 / 2자녀 가구 / 다자녀 가구: 연 7천만원 이하",
                "[만 30세 미만 세대주 특례] 만 30세 미만 단독세대주는 원칙적으로 대출 제외된다.",
                "  단, (1) 민법상 미성년인 형제·자매 중 1인 이상과 같은 세대를 구성하고 주민등록등본상 부양기간",
                "  (합가일 기준)이 계속 6개월 이상이거나, (2) 직계존·비속 중 1인 이상과 같은 세대를 구성하고",
                "  부양기간이 계속 6개월 이상(직계비속은 부양기간 요건 제외)인 경우에는 가능하다.",
                "  만 30세 미만 미혼세대주도 위 (2) 요건을 충족하면 가능하다.",
                "[예비 세대주] 세대주 요건에는 예비 세대주도 포함된다: 세대주의 배우자, 또는 대출접수일 현재",
                "  3개월 이내 결혼으로 세대주가 될 예정인 자.",
                "[계약] 대출 대상주택을 취득하기 위해 주택매매계약을 체결한 자여야 한다.",
                "  상속·증여·재산분할로 주택을 취득하는 경우는 대상이 아니다.",
                "[순자산] 부부합산 순자산 5.11억원(2026년 기준, 통계청 가계금융복지조사 소득 4분위 평균) 이하여야 한다.",
                "[중복대출 금지] 주택도시기금대출 및 은행지원 주택담보대출 이용 중이면 원칙적으로 불가.",
                "  단, 기금 전세자금대출을 받은 경우 대출 실행일 당일 상환 조건부로 가능하다.",
                "  한국주택금융공사 심사 취급 시, 본인 또는 배우자가 동일 물건지로 HF 전세·월세 자금보증을",
                "  이용 중이면 불가하나, 대출실행일까지 전세자금보증을 해지하면 가능하다.",
                "[신용도] 개인신용평점이 일정 점수 이상이어야 하며, 연체·대위변제·부도·금융질서문란 등",
                "  신용정보는 이 서비스가 확인할 수 없으므로 취급 은행에서 확인해야 한다."
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

package com.team.independence.ai.eligibility.model;

import lombok.Getter;

/**
 * 핵심 요건 1건의 규칙 평가 결과.
 *
 * @param requirement 요건 라벨 (예: "소득(기본 5천만원 이하)")
 * @param result      PASS / FAIL / UNKNOWN(확인 필요)
 * @param basis       그렇게 판정한 근거 (예: "40,000,000원", "만 27세 미혼 세대주 — 단독세대 여부 확인 필요")
 */
@Getter
public class RuleFinding {

    private final String requirement;
    private final RuleResult result;
    private final String basis;

    private RuleFinding(String requirement, RuleResult result, String basis) {
        this.requirement = requirement;
        this.result = result;
        this.basis = basis;
    }

    public static RuleFinding pass(String requirement, String basis) {
        return new RuleFinding(requirement, RuleResult.PASS, basis);
    }

    public static RuleFinding fail(String requirement, String basis) {
        return new RuleFinding(requirement, RuleResult.FAIL, basis);
    }

    /** 필수 필드만으로는 판정 불가. basis 에 무엇을 확인해야 하는지 적는다. */
    public static RuleFinding unknown(String requirement, String basis) {
        return new RuleFinding(requirement, RuleResult.UNKNOWN, basis);
    }
}

package com.team.independence.ai.eligibility.model;

import lombok.Getter;

/**
 * 핵심 요건 1건의 규칙 평가 결과. 핵심 요건은 필수 입력값으로만 판정하므로 PASS/FAIL 뿐이다.
 *
 * @param requirement 요건 라벨 (예: "소득(기본 5천만원 이하)")
 * @param result      PASS / FAIL
 * @param basis       그렇게 판정한 근거 (예: "40,000,000원")
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
}

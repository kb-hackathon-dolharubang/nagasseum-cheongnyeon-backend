package com.team.independence.ai.eligibility.model;

/**
 * 핵심 요건 1건에 대한 규칙 평가기의 판정.
 *
 * <p>대부분의 핵심 요건은 필수 입력 필드로만 떨어져 PASS/FAIL 로 끝난다.
 * 다만 일부 정책의 요건은 필수 필드를 넘어서는 사실(예: 디딤돌 "만 30세 미만 미혼 세대주" 특례에서
 * 단독세대 여부·부양가족 존재)을 필요로 한다. 이때는 억지로 PASS/FAIL 을 고르지 않고 {@link #UNKNOWN}
 * (확인 필요)으로 남겨, 무엇을 확인해야 하는지 조언(advice)에서 안내한다.
 */
public enum RuleResult {
    /** 충족 */
    PASS,
    /** 불충족 */
    FAIL,
    /** 필수 필드만으로는 판정 불가 — 사용자가 추가 확인/입력해야 한다 */
    UNKNOWN
}

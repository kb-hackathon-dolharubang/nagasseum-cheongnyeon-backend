package com.team.independence.ai.eligibility.model;

/**
 * 핵심 요건 1건에 대한 규칙 평가기의 판정.
 * 핵심 요건은 전부 필수 입력 필드로만 판정하므로 값이 없는 경우(UNKNOWN)는 없다.
 */
public enum RuleResult {
    /** 충족 */
    PASS,
    /** 불충족 */
    FAIL
}

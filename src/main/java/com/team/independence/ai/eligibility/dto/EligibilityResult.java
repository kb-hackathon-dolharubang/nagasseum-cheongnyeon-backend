package com.team.independence.ai.eligibility.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 정책 적격 심사 결과.
 * <ul>
 *   <li>{@code coreFindings} — 필수 입력값으로 코드가 판정한 핵심 요건별 충족 여부(PASS/FAIL).
 *       전체 적격/부적격 라벨(verdict)은 만들지 않는다. 프론트가 요건별로 표시한다.</li>
 *   <li>{@code advice} — 핵심 요건 외 요건·예외·미입력 항목에 대한 조언. LLM 이 생성.</li>
 * </ul>
 */
@Getter
@Builder
public class EligibilityResult {

    private final String policyId;
    private final String policyName;

    private final List<CoreFindingView> coreFindings;
    private final String advice;

    @Getter
    @Builder
    public static class CoreFindingView {
        private final String requirement;
        /** PASS | FAIL */
        private final String result;
        private final String basis;
    }
}

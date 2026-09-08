package com.team.independence.ai.eligibility.policy;

import com.team.independence.ai.eligibility.model.RuleFinding;
import com.team.independence.ai.eligibility.model.UserProfile;

import java.time.LocalDate;
import java.util.function.BiFunction;

/**
 * 필수 입력값으로 결정론적으로 PASS/FAIL 판정하는 핵심 요건 1건.
 * 이 판정 결과만 리포트의 {@code coreFindings} 로 나간다.
 * 정책마다 이 목록을 직접 작성해 넣는다(정책 소수 고정 전제 — 런타임 LLM 판정 없음).
 */
public interface CoreRequirement {

    String label();

    /** @param asOf 기준일 (성년 판정 등 "대출접수일 현재"에 해당) */
    RuleFinding evaluate(UserProfile profile, LocalDate asOf);

    static CoreRequirement of(String label, BiFunction<UserProfile, LocalDate, RuleFinding> fn) {
        return new CoreRequirement() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public RuleFinding evaluate(UserProfile profile, LocalDate asOf) {
                return fn.apply(profile, asOf);
            }
        };
    }
}

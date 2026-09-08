package com.team.independence.ai.eligibility;

import com.team.independence.ai.eligibility.model.HouseholdRole;
import com.team.independence.ai.eligibility.model.MaritalStatus;
import com.team.independence.ai.eligibility.model.RuleFinding;
import com.team.independence.ai.eligibility.model.RuleResult;
import com.team.independence.ai.eligibility.model.UserProfile;
import com.team.independence.ai.eligibility.policy.DidimdolLoanPolicy;
import com.team.independence.ai.eligibility.service.RuleEvaluator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 내집마련 디딤돌 대출 핵심 요건 규칙 평가기 — LLM 없이 순수 로직만 본다.
 * 버팀목과 다른 점: 소득 기본 상한 6천만원, 세대주 지위에 "만 30세 미만 미혼 세대주" UNKNOWN 분기.
 */
class DidimdolRuleEvaluatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 8);

    private final DidimdolLoanPolicy policy = new DidimdolLoanPolicy();
    private final RuleEvaluator evaluator = new RuleEvaluator();

    private Map<String, RuleFinding> evaluate(UserProfile p) {
        return evaluator.evaluate(policy, p, AS_OF).stream()
                .collect(Collectors.toMap(RuleFinding::getRequirement, Function.identity()));
    }

    /** 만 34세 기혼 세대주, 소득 5천만 — 핵심 요건 4개 전부 PASS 방향. */
    private UserProfile.UserProfileBuilder eligibleBase() {
        return UserProfile.builder()
                .birthDate(LocalDate.of(1992, 4, 1))          // 만 34세
                .combinedAnnualIncome(50_000_000L)
                .allHouseholdMembersHouseless(true)
                .householdRole(HouseholdRole.HEAD)
                .maritalStatus(MaritalStatus.MARRIED);
    }

    @Test
    @DisplayName("핵심 요건은 4개 (성년/세대주지위/무주택/소득)")
    void exactlyFourCoreFindings() {
        List<RuleFinding> findings = evaluator.evaluate(policy, eligibleBase().build(), AS_OF);
        assertThat(findings).extracting(RuleFinding::getRequirement)
                .containsExactly(
                        DidimdolLoanPolicy.R_ADULT_HEAD,
                        DidimdolLoanPolicy.R_HEAD_STATUS,
                        DidimdolLoanPolicy.R_HOUSELESS,
                        DidimdolLoanPolicy.R_INCOME_BASE);
    }

    @Nested
    @DisplayName("소득 기본 상한 6천만원 (버팀목 5천만원과 다름)")
    class Income {
        @Test
        void 경계값_6천만원_PASS() {
            var f = evaluate(eligibleBase().combinedAnnualIncome(60_000_000L).build());
            assertThat(f.get(DidimdolLoanPolicy.R_INCOME_BASE).getResult()).isEqualTo(RuleResult.PASS);
        }

        @Test
        void 초과하면_FAIL() {
            var f = evaluate(eligibleBase().combinedAnnualIncome(60_000_001L).build());
            assertThat(f.get(DidimdolLoanPolicy.R_INCOME_BASE).getResult()).isEqualTo(RuleResult.FAIL);
        }

        @Test
        void 버팀목이면_FAIL일_5천5백만원도_디딤돌은_PASS() {
            var f = evaluate(eligibleBase().combinedAnnualIncome(55_000_000L).build());
            assertThat(f.get(DidimdolLoanPolicy.R_INCOME_BASE).getResult()).isEqualTo(RuleResult.PASS);
        }
    }

    @Nested
    @DisplayName("세대주 지위 — 만 30세 미만 미혼 세대주 특례")
    class HeadStatus {
        @Test
        void 세대원이면_FAIL() {
            var f = evaluate(eligibleBase().householdRole(HouseholdRole.MEMBER).build());
            assertThat(f.get(DidimdolLoanPolicy.R_HEAD_STATUS).getResult()).isEqualTo(RuleResult.FAIL);
        }

        @Test
        void 만30세_이상이면_미혼이어도_PASS() {
            var f = evaluate(eligibleBase()
                    .birthDate(LocalDate.of(1994, 1, 1))     // 만 32세
                    .maritalStatus(MaritalStatus.SINGLE)
                    .build());
            assertThat(f.get(DidimdolLoanPolicy.R_HEAD_STATUS).getResult()).isEqualTo(RuleResult.PASS);
        }

        @Test
        void 만30세_미만_기혼_세대주는_PASS() {
            var f = evaluate(eligibleBase()
                    .birthDate(LocalDate.of(2000, 1, 1))     // 만 26세
                    .maritalStatus(MaritalStatus.MARRIED)
                    .build());
            assertThat(f.get(DidimdolLoanPolicy.R_HEAD_STATUS).getResult()).isEqualTo(RuleResult.PASS);
        }

        @Test
        void 만30세_미만_미혼_세대주는_UNKNOWN() {
            var f = evaluate(eligibleBase()
                    .birthDate(LocalDate.of(2000, 1, 1))     // 만 26세
                    .maritalStatus(MaritalStatus.SINGLE)
                    .build());
            RuleFinding head = f.get(DidimdolLoanPolicy.R_HEAD_STATUS);
            assertThat(head.getResult()).isEqualTo(RuleResult.UNKNOWN);
            assertThat(head.getBasis()).contains("확인 필요");
        }
    }

    @Test
    @DisplayName("만 34세 기혼 세대주·소득 5천만: 핵심 4개 전부 PASS")
    void fully_pass_profile() {
        List<RuleFinding> findings = evaluator.evaluate(policy, eligibleBase().build(), AS_OF);
        assertThat(findings).allMatch(f -> f.getResult() == RuleResult.PASS);
    }
}

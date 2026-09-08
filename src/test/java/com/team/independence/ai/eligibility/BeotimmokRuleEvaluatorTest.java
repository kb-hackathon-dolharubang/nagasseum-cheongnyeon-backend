package com.team.independence.ai.eligibility;

import com.team.independence.ai.eligibility.model.HouseholdRole;
import com.team.independence.ai.eligibility.model.MaritalStatus;
import com.team.independence.ai.eligibility.model.RuleFinding;
import com.team.independence.ai.eligibility.model.RuleResult;
import com.team.independence.ai.eligibility.model.UserProfile;
import com.team.independence.ai.eligibility.policy.BeotimmokJeonseLoanPolicy;
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
 * 버팀목 핵심 요건 규칙 평가기 — LLM 없이 순수 로직만 본다.
 * 핵심 요건은 필수 입력값으로만 판정하므로 결과는 PASS/FAIL 뿐이다(UNKNOWN 없음).
 */
class BeotimmokRuleEvaluatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 8);

    private final BeotimmokJeonseLoanPolicy policy = new BeotimmokJeonseLoanPolicy();
    private final RuleEvaluator evaluator = new RuleEvaluator();

    private Map<String, RuleFinding> evaluate(UserProfile p) {
        return evaluator.evaluate(policy, p, AS_OF).stream()
                .collect(Collectors.toMap(RuleFinding::getRequirement, Function.identity()));
    }

    /** 모든 필수값이 '적격' 방향으로 채워진 기본 프로필. 개별 테스트에서 한 필드만 비틀어 본다. */
    private UserProfile.UserProfileBuilder eligibleBase() {
        return UserProfile.builder()
                .birthDate(LocalDate.of(1993, 4, 1))          // 만 33세
                .combinedAnnualIncome(40_000_000L)
                .allHouseholdMembersHouseless(true)
                .householdRole(HouseholdRole.HEAD)
                .maritalStatus(MaritalStatus.SINGLE);
    }

    @Test
    @DisplayName("핵심 요건은 4개뿐 (성년/세대주지위/무주택/소득)")
    void exactlyFourCoreFindings() {
        List<RuleFinding> findings = evaluator.evaluate(policy, eligibleBase().build(), AS_OF);
        assertThat(findings).extracting(RuleFinding::getRequirement)
                .containsExactly(
                        BeotimmokJeonseLoanPolicy.R_ADULT_HEAD,
                        BeotimmokJeonseLoanPolicy.R_HEAD_STATUS,
                        BeotimmokJeonseLoanPolicy.R_HOUSELESS,
                        BeotimmokJeonseLoanPolicy.R_INCOME_BASE);
    }

    @Nested
    @DisplayName("성년")
    class Adult {
        @Test
        void 만19세_이상이면_PASS() {
            var f = evaluate(eligibleBase().birthDate(LocalDate.of(2007, 9, 8)).build());
            assertThat(f.get(BeotimmokJeonseLoanPolicy.R_ADULT_HEAD).getResult()).isEqualTo(RuleResult.PASS);
        }

        @Test
        void 만19세_미만이면_FAIL() {
            var f = evaluate(eligibleBase().birthDate(LocalDate.of(2010, 1, 1)).build());
            assertThat(f.get(BeotimmokJeonseLoanPolicy.R_ADULT_HEAD).getResult()).isEqualTo(RuleResult.FAIL);
        }
    }

    @Nested
    @DisplayName("세대주 지위")
    class HeadStatus {
        @Test
        void 세대원이면_FAIL() {
            var f = evaluate(eligibleBase().householdRole(HouseholdRole.MEMBER).build());
            assertThat(f.get(BeotimmokJeonseLoanPolicy.R_HEAD_STATUS).getResult()).isEqualTo(RuleResult.FAIL);
        }

        @Test
        void 예비세대주도_PASS() {
            var f = evaluate(eligibleBase().householdRole(HouseholdRole.PROSPECTIVE_HEAD).build());
            assertThat(f.get(BeotimmokJeonseLoanPolicy.R_HEAD_STATUS).getResult()).isEqualTo(RuleResult.PASS);
        }

        @Test
        void 세대주_배우자도_PASS() {
            var f = evaluate(eligibleBase().householdRole(HouseholdRole.SPOUSE_OF_HEAD).build());
            assertThat(f.get(BeotimmokJeonseLoanPolicy.R_HEAD_STATUS).getResult()).isEqualTo(RuleResult.PASS);
        }
    }

    @Test
    @DisplayName("무주택: false 면 FAIL")
    void houseless_false_fail() {
        var f = evaluate(eligibleBase().allHouseholdMembersHouseless(false).build());
        assertThat(f.get(BeotimmokJeonseLoanPolicy.R_HOUSELESS).getResult()).isEqualTo(RuleResult.FAIL);
    }

    @Nested
    @DisplayName("소득 기본 상한 5천만원")
    class Income {
        @Test
        void 경계값_5천만원_PASS() {
            var f = evaluate(eligibleBase().combinedAnnualIncome(50_000_000L).build());
            assertThat(f.get(BeotimmokJeonseLoanPolicy.R_INCOME_BASE).getResult()).isEqualTo(RuleResult.PASS);
        }

        @Test
        void 초과하면_FAIL() {
            var f = evaluate(eligibleBase().combinedAnnualIncome(50_000_001L).build());
            assertThat(f.get(BeotimmokJeonseLoanPolicy.R_INCOME_BASE).getResult()).isEqualTo(RuleResult.FAIL);
        }
    }

    @Test
    @DisplayName("완전한 적격 프로필: 핵심 4개 전부 PASS")
    void fully_eligible_profile() {
        List<RuleFinding> findings = evaluator.evaluate(policy, eligibleBase().build(), AS_OF);
        assertThat(findings).allMatch(f -> f.getResult() == RuleResult.PASS);
    }
}

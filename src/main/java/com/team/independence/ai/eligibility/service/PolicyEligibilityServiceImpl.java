package com.team.independence.ai.eligibility.service;

import com.team.independence.ai.eligibility.dto.EligibilityRequest;
import com.team.independence.ai.eligibility.dto.EligibilityResult;
import com.team.independence.ai.eligibility.model.RuleFinding;
import com.team.independence.ai.eligibility.model.UserProfile;
import com.team.independence.ai.eligibility.policy.PolicyDefinition;
import com.team.independence.ai.eligibility.policy.PolicyRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 정책 적격 심사 오케스트레이션:
 *   요청 → UserProfile 변환 → 정책 조회 → 핵심 요건 판정(코드) → 조언 생성(LLM) → 결과.
 *
 * <p>플로우가 정책에 독립이므로, 정책이 늘어도 이 클래스는 바뀌지 않는다
 * (새 {@link PolicyDefinition} 구현체만 추가).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyEligibilityServiceImpl implements PolicyEligibilityService {

    private final PolicyRegistry policyRegistry;
    private final RuleEvaluator ruleEvaluator;
    private final LlmEligibilityAssessor llmAssessor;

    @Override
    public EligibilityResult assess(EligibilityRequest request) {
        PolicyDefinition policy = policyRegistry.get(request.getPolicyId());
        UserProfile profile = request.toUserProfile();
        LocalDate asOf = LocalDate.now();

        List<RuleFinding> coreFindings = ruleEvaluator.evaluate(policy, profile, asOf);
        String advice = llmAssessor.generateAdvice(policy, profile, coreFindings, asOf);

        log.info("정책 적격 심사 완료: policy={}, coreFindings={}", policy.id(), coreFindings.size());

        return EligibilityResult.builder()
                .policyId(policy.id())
                .policyName(policy.name())
                .coreFindings(coreFindings.stream()
                        .map(f -> EligibilityResult.CoreFindingView.builder()
                                .requirement(f.getRequirement())
                                .result(f.getResult().name())
                                .basis(f.getBasis())
                                .build())
                        .collect(Collectors.toList()))
                .advice(advice)
                .build();
    }
}

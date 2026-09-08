package com.team.independence.ai.eligibility.service;

import com.team.independence.ai.eligibility.model.RuleFinding;
import com.team.independence.ai.eligibility.model.UserProfile;
import com.team.independence.ai.eligibility.policy.PolicyDefinition;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 정책의 핵심 요건을 순서대로 PASS/FAIL 판정한다. LLM 을 타지 않는 순수 로직.
 */
@Component
public class RuleEvaluator {

    public List<RuleFinding> evaluate(PolicyDefinition policy, UserProfile profile, LocalDate asOf) {
        List<RuleFinding> findings = new ArrayList<>();
        policy.coreRequirements().forEach(req -> findings.add(req.evaluate(profile, asOf)));
        return findings;
    }
}

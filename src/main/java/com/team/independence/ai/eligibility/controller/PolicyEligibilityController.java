package com.team.independence.ai.eligibility.controller;

import com.team.independence.ai.eligibility.dto.EligibilityRequest;
import com.team.independence.ai.eligibility.dto.EligibilityResult;
import com.team.independence.ai.eligibility.service.PolicyEligibilityService;
import com.team.independence.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 정책 적격 심사 (AI 레이어). 팀원 정책 도메인이 호출한다.
 * 요청은 자체 완결적이다 (심사에 필요한 값을 모두 바디로 받는다).
 */
@RestController
@RequestMapping("/api/v1/ai/policy-eligibility")
@RequiredArgsConstructor
public class PolicyEligibilityController {

    private final PolicyEligibilityService policyEligibilityService;

    @PostMapping
    public ApiResponse<EligibilityResult> assess(@Valid @RequestBody EligibilityRequest request) {
        return ApiResponse.ok(policyEligibilityService.assess(request));
    }
}

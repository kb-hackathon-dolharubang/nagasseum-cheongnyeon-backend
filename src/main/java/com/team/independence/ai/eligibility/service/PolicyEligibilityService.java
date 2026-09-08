package com.team.independence.ai.eligibility.service;

import com.team.independence.ai.eligibility.dto.EligibilityRequest;
import com.team.independence.ai.eligibility.dto.EligibilityResult;

public interface PolicyEligibilityService {

    EligibilityResult assess(EligibilityRequest request);
}

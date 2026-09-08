package com.team.independence.ai.eligibility.policy;

import com.team.independence.common.exception.BusinessException;
import com.team.independence.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 등록된 모든 {@link PolicyDefinition} 을 id 로 색인한다.
 * 스프링이 구현체 빈을 전부 주입 → 새 정책은 {@code @Component} 하나 추가로 끝.
 */
@Component
public class PolicyRegistry {

    private final Map<String, PolicyDefinition> byId = new LinkedHashMap<>();

    public PolicyRegistry(List<PolicyDefinition> definitions) {
        for (PolicyDefinition d : definitions) {
            PolicyDefinition prev = byId.put(d.id(), d);
            if (prev != null) {
                throw new IllegalStateException("정책 id 중복: " + d.id());
            }
        }
    }

    public PolicyDefinition get(String id) {
        PolicyDefinition d = byId.get(id);
        if (d == null) {
            throw new BusinessException(ErrorCode.POLICY_NOT_FOUND, "지원하지 않는 정책 id: " + id);
        }
        return d;
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }
}

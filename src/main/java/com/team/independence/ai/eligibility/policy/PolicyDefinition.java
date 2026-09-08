package com.team.independence.ai.eligibility.policy;

import java.util.List;

/**
 * 정책 1개의 적격 심사 정의. 새 정책 추가 = 이 인터페이스 구현체를 하나 더 등록하면 끝
 * ({@link PolicyRegistry} 가 스프링에서 모두 모아 id 로 색인).
 *
 * <p>판정과 조언이 분리된다:
 * <ul>
 *   <li>{@link #coreRequirements()} — 필수 입력값으로 코드가 PASS/FAIL 판정. 리포트 coreFindings 로 나감.</li>
 *   <li>{@link #adviceContext()} — 핵심 요건 외 요건·예외조항을 서술한 참고 텍스트. 이 정책이 LLM 에게
 *       "무엇을 조언에 담아야 하는지" 알려주는 배경지식이다. (순자산 상한, 중복대출 금지, 소득 예외상한,
 *       예비세대주 정의, 공공임대 예외, 신용도 등)</li>
 * </ul>
 * 규칙평가기·LLM 조언 생성기·서비스 골격은 정책에 독립이다.
 */
public interface PolicyDefinition {

    /** URL/요청에서 쓰는 식별자 (예: "beotimmok-jeonse"). */
    String id();

    /** 사람이 읽는 이름 (예: "버팀목 전세자금대출"). */
    String name();

    /** 규칙평가기가 순서대로 판정할 핵심 요건 (필수 입력값 기반, PASS/FAIL). */
    List<CoreRequirement> coreRequirements();

    /** LLM 조언 생성에 넣을 정책 배경지식(핵심 요건 외 요건·예외조항 서술). */
    String adviceContext();
}

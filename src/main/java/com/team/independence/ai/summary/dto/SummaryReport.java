package com.team.independence.ai.summary.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 상담 요약 리포트. {@code summary} 와 {@code result} 는 항상 채워지고,
 * 배열 3개는 근거가 없으면 빈 배열이다(억지 채움 방지).
 */
@Getter
@Builder
public class SummaryReport {

    /** 상담 요약 — 전체 개요 3~5문장. */
    private final String summary;

    /** 핵심 고민 — 신청인이 상담에서 풀고 싶어한 것. 없으면 빈 리스트. */
    private final List<String> mainConcerns;

    /** 함께 확인한 내용 — 상담에서 검토·비교한 항목. 없으면 빈 리스트. */
    private final List<String> discussionPoints;

    /** 상담 결과 — 어떤 방향으로 정리됐는지 1~3문장. */
    private final String result;

    /** 상담에서 제안된 내용 — 검토·실행이 제안된 항목. 없으면 빈 리스트. */
    private final List<String> recommendations;
}

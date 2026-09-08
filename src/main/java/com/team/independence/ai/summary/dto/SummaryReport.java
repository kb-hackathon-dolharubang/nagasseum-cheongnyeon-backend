package com.team.independence.ai.summary.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 채팅 요약 리포트 (느슨한 고정 스키마).
 * {@code summary} 만 항상 채워지고, 나머지는 대화에 없으면 비어 있을 수 있다(억지 채움 방지).
 */
@Getter
@Builder
public class SummaryReport {

    /** 필수 — 상담 개요 3~5문장. */
    private final String summary;

    /** 대화에서 파악된 주거 목표. 없으면 null. */
    private final String housingGoal;

    /** 논의된 조건(예산·지역·시기·대출 등). 없으면 빈 리스트. */
    private final List<String> discussedConditions;

    /** 액션 아이템. 없으면 빈 리스트. */
    private final List<ActionItem> actionItems;

    /** 다음 단계/다음 상담 예정. 없으면 null. */
    private final String nextSteps;

    @Getter
    @Builder
    public static class ActionItem {
        /** USER | COUNSELOR */
        private final String who;
        private final String task;
    }
}

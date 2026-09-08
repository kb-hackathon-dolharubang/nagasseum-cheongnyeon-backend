package com.team.independence.ai.eligibility.model;

/** 세대 내 지위. 버팀목 (세대주) 요건 판정에 쓰인다. */
public enum HouseholdRole {
    /** 세대주 본인 */
    HEAD,
    /** 세대주의 배우자 (요건상 세대주로 간주) */
    SPOUSE_OF_HEAD,
    /** 예비 세대주 (대출실행일 1개월 내 분가/합가, 또는 3개월 내 결혼으로 세대주 예정) */
    PROSPECTIVE_HEAD,
    /** 단순 세대원 (세대주 요건 불충족) */
    MEMBER
}

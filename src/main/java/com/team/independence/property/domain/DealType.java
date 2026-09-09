package com.team.independence.property.domain;

/**
 * 거래 유형.
 *
 * <p>MyBatis 기본 EnumTypeHandler가 상수명을 그대로 저장/조회하므로
 * 상수명 = deal_type 컬럼 값이다. 이름을 바꾸면 기존 데이터와 어긋난다.
 */
public enum DealType {
    /** 전세 — 보증금만 있고 월세 없음 */
    JEONSE,
    /** 월세 — 보증금 + 월세 */
    WOLSE,
    /** 매매 — 거래금액이 deposit에 저장되고 monthly_rent는 0 */
    TRADE;

    /**
     * 전월세 데이터에서 월세액으로 유형을 판별한다.
     * 매매 API 응답에는 사용하지 않는다 — 매매는 {@link #TRADE}를 직접 지정한다.
     */
    public static DealType fromMonthlyRent(long monthlyRent) {
        return monthlyRent == 0 ? JEONSE : WOLSE;
    }
}

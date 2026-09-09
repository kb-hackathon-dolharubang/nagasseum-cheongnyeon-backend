package com.team.independence.property.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.independence.property.domain.DealType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * DealType 열거형 동작 검증.
 *
 * <p>fromMonthlyRent()는 국토부 전월세 API 응답에서만 쓰인다.
 * TRADE는 월세액으로 파생하지 않고 매매 수집 시 직접 지정한다.
 *
 * <p>한계: 국토부 API는 계약 유형 필드를 제공하지 않고 보증금·월세액만 내려준다.
 * 반전세(보증금 + 소액 월세)처럼 실질은 전세에 가까운 계약도 월세액이 있으면
 * WOLSE로 분류되는 문제가 있다. 계약 유형 필드가 생기면 이 메서드를 재검토해야 한다.
 */
class DealTypeTest {

    @Test
    @DisplayName("전월세 API 컨텍스트에서 월세액 0은 JEONSE — 매매는 이 메서드를 거치지 않으므로 TRADE가 반환될 여지가 없다")
    void fromMonthlyRent_전월세API에서_월세0이면_JEONSE() {
        assertThat(DealType.fromMonthlyRent(0)).isEqualTo(DealType.JEONSE);
    }

    @Test
    @DisplayName("fromMonthlyRent는 TRADE를 반환하지 않는다 — TRADE는 매매 API 수집 시 직접 지정")
    void fromMonthlyRent가_TRADE를_반환하지_않음() {
        for (long rent : new long[]{0, 1, 1_000_000}) {
            assertThat(DealType.fromMonthlyRent(rent)).isNotEqualTo(DealType.TRADE);
        }
    }

    @Test
    @DisplayName("DealType에 TRADE 상수가 존재한다")
    void TRADE_상수_존재() {
        assertThat(DealType.valueOf("TRADE")).isEqualTo(DealType.TRADE);
    }

    @Test
    @DisplayName("DealType.values()에 JEONSE, WOLSE, TRADE 세 값이 모두 포함된다")
    void values_세_유형_포함() {
        assertThat(DealType.values())
                .containsExactlyInAnyOrder(DealType.JEONSE, DealType.WOLSE, DealType.TRADE);
    }
}

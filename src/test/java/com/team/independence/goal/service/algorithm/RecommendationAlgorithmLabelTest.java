package com.team.independence.goal.service.algorithm;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.independence.goal.service.RecommendationAlgorithm;
import com.team.independence.property.domain.DealType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RecommendationAlgorithm.label(DealType) 레이블 매핑 검증.
 */
class RecommendationAlgorithmLabelTest {

    @Test
    @DisplayName("JEONSE → '전세'")
    void label_JEONSE() {
        assertThat(RecommendationAlgorithm.label(DealType.JEONSE)).isEqualTo("전세");
    }

    @Test
    @DisplayName("WOLSE → '월세'")
    void label_WOLSE() {
        assertThat(RecommendationAlgorithm.label(DealType.WOLSE)).isEqualTo("월세");
    }

    @Test
    @DisplayName("TRADE → '매매'")
    void label_TRADE() {
        assertThat(RecommendationAlgorithm.label(DealType.TRADE)).isEqualTo("매매");
    }
}

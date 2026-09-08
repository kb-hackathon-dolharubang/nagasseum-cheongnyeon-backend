package com.team.independence.property.dto;

import com.team.independence.property.domain.DealType;
import com.team.independence.property.domain.HousingType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** walk-forward 백테스팅 조합 탐색용 mapper 프로젝션 */
@Getter
@Setter
@NoArgsConstructor
public class BacktestComboRow {
    private String regionCode;
    private HousingType housingType;
    private DealType dealType;
}

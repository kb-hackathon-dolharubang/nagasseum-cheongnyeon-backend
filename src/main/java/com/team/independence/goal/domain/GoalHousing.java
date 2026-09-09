package com.team.independence.goal.domain;

import com.team.independence.property.domain.DealType;
import com.team.independence.property.domain.HousingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 기본 생성자와 setter는 MyBatis가 조회 결과를 매핑할 때 쓴다. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalHousing {
    private Long goalId;
    private String regionCode;
    /** 희망 읍면동 법정동코드. null이면 regionCode가 가리키는 구 단위 전체. */
    private String dongCode;
    private HousingType housingType;
    private DealType dealType;
    private Integer areaMin;
    private Integer areaMax;
    private Long depositMin;
    private Long depositMax;
    private Long monthlyRentMin;
    private Long monthlyRentMax;
}

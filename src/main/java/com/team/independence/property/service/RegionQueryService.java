package com.team.independence.property.service;

public interface RegionQueryService {

    /** 시/도 + 군/구 이름으로 region_code를 조회한다. 없으면 BusinessException(REGION_NOT_FOUND). */
    String resolveRegionCode(String sido, String sigungu);

    /** region_code로 지역 전체 지명을 조회한다. 없으면 BusinessException(REGION_NOT_FOUND). */
    String resolveRegionName(String regionCode);

    /** 법정동코드(10자리)로 읍면동명을 조회한다. 없으면 null. */
    String resolveDongName(String dongCode);
}

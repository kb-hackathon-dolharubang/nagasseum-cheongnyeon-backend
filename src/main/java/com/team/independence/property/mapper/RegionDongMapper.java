package com.team.independence.property.mapper;

import com.team.independence.property.domain.RegionDong;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegionDongMapper {
    /** 시군구 코드로 해당 구의 읍면동 목록 조회 (드롭다운용) */
    List<RegionDong> findBySigunguCode(@Param("sigunguCode") String sigunguCode);

    /** 법정동코드로 단건 조회 */
    RegionDong findByCode(@Param("code") String code);
}

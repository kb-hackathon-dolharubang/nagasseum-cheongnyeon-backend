package com.team.independence.property.service;

import com.team.independence.common.exception.BusinessException;
import com.team.independence.common.exception.ErrorCode;
import com.team.independence.property.domain.RegionDong;
import com.team.independence.property.mapper.RegionDongMapper;
import com.team.independence.property.mapper.RegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegionQueryServiceImpl implements RegionQueryService {

    private final RegionMapper regionMapper;
    private final RegionDongMapper regionDongMapper;

    @Override
    @Transactional(readOnly = true)
    public String resolveRegionCode(String sido, String sigungu) {
        String regionCode = regionMapper.findCodeBySidoAndSigungu(sido, sigungu);
        if (regionCode == null) {
            throw new BusinessException(ErrorCode.REGION_NOT_FOUND);
        }
        return regionCode;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveRegionName(String regionCode) {
        String regionName = regionMapper.findFullNameByCode(regionCode);
        if (regionName == null) {
            throw new BusinessException(ErrorCode.REGION_NOT_FOUND);
        }
        return regionName;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveDongName(String dongCode) {
        if (dongCode == null) {
            return null;
        }
        RegionDong dong = regionDongMapper.findByCode(dongCode);
        return dong != null ? dong.getDongName() : null;
    }
}

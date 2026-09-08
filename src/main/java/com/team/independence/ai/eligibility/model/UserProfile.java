package com.team.independence.ai.eligibility.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.Period;

/**
 * 정규화된 신청인 속성. 정책 심사 레이어의 입력 계약이다.
 * 팀원 정책 도메인이 화면에서 모은 값을 {@code EligibilityRequest} 로 넘기면 서비스가 이 모델로 변환한다.
 *
 * <p>필수(5) — 정책 공통으로 항상 쓰이는 값. 핵심 요건 판정에 쓰인다:
 *   birthDate, combinedAnnualIncome, allHouseholdMembersHouseless, householdRole, maritalStatus
 * <p>선택 — 없으면 그 요건은 핵심 판정에서 빠지고 조언(advice)에서 "입력하면 확인 가능" 으로 안내:
 *   combinedNetAsset, marriageDate, numberOfChildren, livingInPublicRentalHousing,
 *   usingFundLoan, usingJeonseOrMortgageLoan
 */
@Getter
@Builder
public class UserProfile {

    // 필수
    private final LocalDate birthDate;
    private final Long combinedAnnualIncome;
    private final Boolean allHouseholdMembersHouseless;
    private final HouseholdRole householdRole;
    private final MaritalStatus maritalStatus;

    // 선택
    private final Long combinedNetAsset;
    private final LocalDate marriageDate;
    private final Integer numberOfChildren;
    private final Boolean livingInPublicRentalHousing;
    private final Boolean usingFundLoan;
    private final Boolean usingJeonseOrMortgageLoan;

    /** 기준일 현재 만 나이. */
    public int ageAsOf(LocalDate asOf) {
        return Period.between(birthDate, asOf).getYears();
    }
}

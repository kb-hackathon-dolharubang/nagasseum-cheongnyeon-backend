package com.team.independence.ai.eligibility.dto;

import com.team.independence.ai.eligibility.model.HouseholdRole;
import com.team.independence.ai.eligibility.model.MaritalStatus;
import com.team.independence.ai.eligibility.model.UserProfile;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * 정책 적격 심사 요청. 팀원 정책 도메인이 화면에서 모은 값을 이 모양으로 조립해 넘긴다(= 경계 어댑터).
 *
 * <p>필수(누락 시 400) — 정책 공통으로 항상 쓰이는 값:
 *   birthDate, combinedAnnualIncome, allHouseholdMembersHouseless, householdRole, maritalStatus
 * <p>선택(누락 시 핵심 판정에서 빠지고 advice 에서 안내):
 *   combinedNetAsset, marriageDate, numberOfChildren, livingInPublicRentalHousing,
 *   usingFundLoan, usingJeonseOrMortgageLoan
 */
@Getter
@NoArgsConstructor
public class EligibilityRequest {

    @NotBlank
    private String policyId;

    // ===== 필수 =====
    @NotNull
    private LocalDate birthDate;

    @NotNull
    @PositiveOrZero
    private Long combinedAnnualIncome;

    @NotNull
    private Boolean allHouseholdMembersHouseless;

    @NotNull
    private HouseholdRole householdRole;

    @NotNull
    private MaritalStatus maritalStatus;

    // ===== 선택 =====
    @PositiveOrZero
    private Long combinedNetAsset;

    private LocalDate marriageDate;

    @PositiveOrZero
    private Integer numberOfChildren;

    private Boolean livingInPublicRentalHousing;

    private Boolean usingFundLoan;

    private Boolean usingJeonseOrMortgageLoan;

    public UserProfile toUserProfile() {
        return UserProfile.builder()
                .birthDate(birthDate)
                .combinedAnnualIncome(combinedAnnualIncome)
                .allHouseholdMembersHouseless(allHouseholdMembersHouseless)
                .householdRole(householdRole)
                .maritalStatus(maritalStatus)
                .combinedNetAsset(combinedNetAsset)
                .marriageDate(marriageDate)
                .numberOfChildren(numberOfChildren)
                .livingInPublicRentalHousing(livingInPublicRentalHousing)
                .usingFundLoan(usingFundLoan)
                .usingJeonseOrMortgageLoan(usingJeonseOrMortgageLoan)
                .build();
    }
}

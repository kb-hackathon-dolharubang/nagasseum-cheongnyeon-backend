package com.team.independence.ai.summary.dto;

import com.team.independence.ai.summary.model.SenderType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 상담 요약 요청. 팀원 상담 도메인이 예약·상담정보·대화기록을 이 모양으로 조립해 넘긴다(= 경계 어댑터).
 * 프론트가 보내는 스키마를 그대로 받는다. 코드값(OFFICETEL/JEONSE 등)·한글 문자열을 정규화하지 않고
 * 프롬프트에 그대로 넣는다. 실명·연락처 같은 개인식별정보는 담지 않는 것을 전제로 한다.
 *
 * <p>필수: reservationId, consultationType, category, consultInfo, messages
 * <p>선택: preConsultationQuestion, diagnosis (목표 진단 연계 상담일 때만)
 */
@Getter
@NoArgsConstructor
public class SummaryRequest {

    @NotNull
    private Long reservationId;

    /** 상담 유형 (예: GENERAL). */
    @NotBlank
    private String consultationType;

    /** 상담 카테고리 (예: HOUSING). */
    @NotBlank
    private String category;

    /** 사전 질문. 없을 수 있다. */
    private String preConsultationQuestion;

    @NotNull
    @Valid
    private ConsultInfo consultInfo;

    /** 목표 진단 연계 상담이면 채워진다. 아니면 null. */
    @Valid
    private Diagnosis diagnosis;

    @NotEmpty
    @Valid
    private List<Message> messages;

    // ===== nested =====

    @Getter
    @NoArgsConstructor
    public static class ConsultInfo {
        @Valid
        private HousingPreference housingPreference;
        /** 현재 자산(원). */
        private Long currentAsset;
        /** 월 저축 가능액(원). */
        private Long monthlySaving;
        /** 목표 시점 "yyyy-MM". */
        private String targetDate;
        /** 대출 활용 의향 (예: UNDECIDED / YES / NO). */
        private String loanPreference;
    }

    @Getter
    @NoArgsConstructor
    public static class HousingPreference {
        private String province;
        private String district;
        private String neighborhood;
        /** 주택 유형 코드 (예: OFFICETEL). */
        private String housingType;
        /** 거래 유형 코드 (예: JEONSE). */
        private String transactionType;
        @Valid
        private AreaRange areaRange;
    }

    @Getter
    @NoArgsConstructor
    public static class AreaRange {
        private Integer min;
        private Integer max;
        /** 표시용 라벨 (예: "10~20평"). */
        private String label;
    }

    @Getter
    @NoArgsConstructor
    public static class Diagnosis {
        @Valid
        private DiagnosisCondition originalCondition;
        @Valid
        private DiagnosisCondition recommendedCondition;
        /** 목표 시점 "yyyy-MM". */
        private String targetDate;
        /** 추천 월 저축액(원). */
        private Long recommendedMonthlySaving;
    }

    @Getter
    @NoArgsConstructor
    public static class DiagnosisCondition {
        /** 한글 표기 (예: "서울 마포구"). */
        private String region;
        /** 한글 표기 (예: "오피스텔"). */
        private String housingType;
        /** 한글 표기 (예: "전세"). */
        private String transactionType;
        /** 한글 표기 (예: "10~20평"). */
        private String area;
    }

    @Getter
    @NoArgsConstructor
    public static class Message {
        @NotNull
        private SenderType senderType;

        @NotBlank
        private String content;

        /** 발화 시각. 시간순 정렬·맥락에만 쓰인다. */
        private LocalDateTime createdAt;
    }
}

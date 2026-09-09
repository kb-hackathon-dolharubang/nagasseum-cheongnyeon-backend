package com.team.independence.consultation.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
public class ConsultationReservation {
    private Long reservationId;
    private Long userId;
    private Long counselorId;
    private ConsultationType consultationType;
    private ConsultationCategory category;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private String requestMessage;
    /** 이번 상담에 공유하기로 확정한 사용자 정보 스냅샷(JSON 문자열) */
    private String consultInfoJson;
    /** GOAL_DIAGNOSIS 상담일 때만 값이 있다(JSON 문자열) */
    private String diagnosisJson;
    /** 상담 요약 리포트 스냅샷(JSON 문자열). 종료 시 1회 생성, 성공(COMPLETED)일 때만 값이 있다 */
    private String reportJson;
    /** COMPLETED / FAILED / NO_MESSAGES. 상담 종료 시 결정, 종료 전엔 null */
    private ReportStatus reportStatus;
    private ConsultationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;
}

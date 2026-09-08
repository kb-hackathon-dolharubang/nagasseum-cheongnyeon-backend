package com.team.independence.consultation.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

/** 상담사 홈 화면용. member 테이블을 조인해 신청자 이름(userName)까지 내려준다. */
@Getter
@Builder
public class ConsultationCounselorReservationResponse {
    private Long reservationId;
    private Long userId;
    private String userName;
    private String consultationType;
    private String category;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private String status;
}

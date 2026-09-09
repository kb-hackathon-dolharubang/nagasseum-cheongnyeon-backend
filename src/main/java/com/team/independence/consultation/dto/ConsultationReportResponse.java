package com.team.independence.consultation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConsultationReportResponse {
    /** COMPLETED / FAILED */
    private String status;
    private String summary;
    private List<String> mainConcerns;
    private List<String> discussionPoints;
    private String result;
    private List<String> recommendations;
}

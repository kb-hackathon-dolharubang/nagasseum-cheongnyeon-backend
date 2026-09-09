package com.team.independence.consultation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team.independence.ai.summary.dto.SummaryReport;
import com.team.independence.ai.summary.dto.SummaryRequest;
import com.team.independence.ai.summary.service.ChatSummaryService;
import com.team.independence.common.exception.BusinessException;
import com.team.independence.common.exception.ErrorCode;
import com.team.independence.consultation.domain.ConsultationCategory;
import com.team.independence.consultation.domain.ConsultationMessage;
import com.team.independence.consultation.domain.ConsultationReservation;
import com.team.independence.consultation.domain.ConsultationStatus;
import com.team.independence.consultation.domain.ConsultationType;
import com.team.independence.consultation.dto.ConsultationCounselorReservationResponse;
import com.team.independence.consultation.dto.ConsultationEndResponse;
import com.team.independence.consultation.dto.ConsultationReportResponse;
import com.team.independence.consultation.dto.ConsultationReservationCreateRequest;
import com.team.independence.consultation.dto.ConsultationReservationResponse;
import com.team.independence.consultation.dto.ConsultationUserReservationResponse;
import com.team.independence.consultation.mapper.ConsultationMapper;
import com.team.independence.consultation.mapper.ConsultationMessageMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationMapper consultationMapper;
    private final ConsultationMessageMapper consultationMessageMapper;
    private final ChatSummaryService chatSummaryService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ConsultationReservationResponse createReservation(ConsultationReservationCreateRequest request) {
        ConsultationReservation reservation = ConsultationReservation.builder()
                .userId(request.getUserId())
                .counselorId(request.getCounselorId())
                .consultationType(ConsultationType.valueOf(request.getConsultationType()))
                .category(ConsultationCategory.valueOf(request.getCategory()))
                .reservationDate(request.getReservationDate())
                .reservationTime(request.getReservationTime())
                .requestMessage(request.getRequestMessage())
                .consultInfoJson(toJson(request.getConsultInfo()))
                .diagnosisJson(request.getDiagnosis() != null ? toJson(request.getDiagnosis()) : null)
                .status(ConsultationStatus.RESERVED)
                .build();

        consultationMapper.insert(reservation);

        return ConsultationReservationResponse.builder()
                .reservationId(reservation.getReservationId())
                .status(reservation.getStatus().name())
                .reservationDate(reservation.getReservationDate())
                .reservationTime(reservation.getReservationTime())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationUserReservationResponse> getUserReservations(Long userId) {
        return consultationMapper.findByUserId(userId).stream()
                .map(r -> ConsultationUserReservationResponse.builder()
                        .reservationId(r.getReservationId())
                        .counselorId(r.getCounselorId())
                        .consultationType(r.getConsultationType().name())
                        .category(r.getCategory().name())
                        .reservationDate(r.getReservationDate())
                        .reservationTime(r.getReservationTime())
                        .status(r.getStatus().name())
                        .requestMessage(r.getRequestMessage())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationCounselorReservationResponse> getCounselorReservations(Long counselorId) {
        return consultationMapper.findByCounselorId(counselorId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationReservation findReservation(Long reservationId) {
        ConsultationReservation reservation = consultationMapper.findById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.CONSULTATION_NOT_FOUND);
        }
        return reservation;
    }

    @Override
    @Transactional
    public void startIfReserved(Long reservationId) {
        consultationMapper.updateStatusToInProgress(reservationId);
    }

    @Override
    @Transactional
    public ConsultationEndResponse endConsultation(Long reservationId) {
        ConsultationReservation reservation = findReservation(reservationId);
        if (reservation.getStatus() == ConsultationStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CONSULTATION_ALREADY_COMPLETED, "이미 종료된 상담입니다.");
        }

        consultationMapper.updateStatusToCompleted(reservationId);
        ConsultationReservation ended = consultationMapper.findById(reservationId);

        generateReport(ended);

        return ConsultationEndResponse.builder()
                .reservationId(ended.getReservationId())
                .status(ended.getStatus().name())
                .endedAt(ended.getEndedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationReportResponse getReport(Long reservationId) {
        ConsultationReservation reservation = findReservation(reservationId);
        if (reservation.getReportJson() == null) {
            return ConsultationReportResponse.builder().status("FAILED").build();
        }
        return parseReport(reservation.getReportJson());
    }

    /**
     * 상담 종료 시 AI 요약 리포트를 동기로 생성해 저장한다. 요약할 메시지가 없으면 건너뛰고,
     * AI 호출/파싱이 실패해도 예외를 던지지 않는다(상담 종료 자체를 막지 않기 위함) —
     * 이 경우 report_json은 null로 남고, 리포트 화면에서는 FAILED로 노출된다.
     */
    private void generateReport(ConsultationReservation reservation) {
        List<ConsultationMessage> messages =
                consultationMessageMapper.findByReservationId(reservation.getReservationId());
        if (messages.isEmpty()) {
            return;
        }

        try {
            SummaryRequest request = buildSummaryRequest(reservation, messages);
            SummaryReport report = chatSummaryService.summarize(request);
            consultationMapper.updateReportJson(
                    reservation.getReservationId(), objectMapper.writeValueAsString(report));
        } catch (Exception e) {
            log.error("상담 리포트 생성 실패: reservationId={}", reservation.getReservationId(), e);
        }
    }

    /**
     * consultInfoJson/diagnosisJson은 예약 생성 시 프론트가 보낸 것을 그대로 저장해둔 스냅샷이라
     * SummaryRequest.ConsultInfo/Diagnosis와 필드가 이미 동일하다 - 파싱해서 그대로 끼워 넣는다.
     * SummaryRequest는 builder/setter가 없어 @RequestBody와 같은 경로(ObjectMapper 역직렬화)로만
     * 만들 수 있다.
     */
    private SummaryRequest buildSummaryRequest(
            ConsultationReservation reservation, List<ConsultationMessage> messages)
            throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("reservationId", reservation.getReservationId());
        root.put("consultationType", reservation.getConsultationType().name());
        root.put("category", reservation.getCategory().name());
        root.put("preConsultationQuestion", reservation.getRequestMessage());
        root.set("consultInfo", objectMapper.readTree(reservation.getConsultInfoJson()));
        if (reservation.getDiagnosisJson() != null) {
            root.set("diagnosis", objectMapper.readTree(reservation.getDiagnosisJson()));
        }

        ArrayNode messagesNode = root.putArray("messages");
        for (ConsultationMessage message : messages) {
            ObjectNode messageNode = messagesNode.addObject();
            messageNode.put("senderType", message.getSenderType().name());
            messageNode.put("content", message.getContent());
            if (message.getCreatedAt() != null) {
                messageNode.put("createdAt", message.getCreatedAt().toString());
            }
        }

        return objectMapper.treeToValue(root, SummaryRequest.class);
    }

    /** ChatSummaryServiceImpl.parse()와 같은 방식 - SummaryReport는 builder만 있어 readValue로 바로 못 만든다. */
    private ConsultationReportResponse parseReport(String reportJson) {
        try {
            JsonNode root = objectMapper.readTree(reportJson);
            return ConsultationReportResponse.builder()
                    .status("COMPLETED")
                    .summary(root.path("summary").asText(null))
                    .mainConcerns(toStringList(root.path("mainConcerns")))
                    .discussionPoints(toStringList(root.path("discussionPoints")))
                    .result(root.path("result").asText(null))
                    .recommendations(toStringList(root.path("recommendations")))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("저장된 리포트 파싱 실패: {}", e.getMessage());
            return ConsultationReportResponse.builder().status("FAILED").build();
        }
    }

    private static List<String> toStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                result.add(item.asText());
            }
        }
        return result;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "상담 정보를 저장하는 중 오류가 발생했습니다.");
        }
    }
}

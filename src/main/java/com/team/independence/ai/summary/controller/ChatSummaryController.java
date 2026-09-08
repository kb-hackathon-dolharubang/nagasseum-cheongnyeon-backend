package com.team.independence.ai.summary.controller;

import com.team.independence.ai.summary.dto.SummaryReport;
import com.team.independence.ai.summary.dto.SummaryRequest;
import com.team.independence.ai.summary.service.ChatSummaryService;
import com.team.independence.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 채팅 요약 (AI 레이어). 팀원 상담 채팅 도메인이 대화 기록을 넘겨 호출한다.
 */
@RestController
@RequestMapping("/api/v1/ai/chat-summary")
@RequiredArgsConstructor
public class ChatSummaryController {

    private final ChatSummaryService chatSummaryService;

    @PostMapping
    public ApiResponse<SummaryReport> summarize(@Valid @RequestBody SummaryRequest request) {
        return ApiResponse.ok(chatSummaryService.summarize(request));
    }
}

package com.team.independence.ai.summary.service;

import com.team.independence.ai.summary.dto.SummaryReport;
import com.team.independence.ai.summary.dto.SummaryRequest;

public interface ChatSummaryService {

    SummaryReport summarize(SummaryRequest request);
}

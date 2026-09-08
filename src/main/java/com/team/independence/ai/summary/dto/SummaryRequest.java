package com.team.independence.ai.summary.dto;

import com.team.independence.ai.summary.model.MessageRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 요약 요청. 팀원 채팅 도메인이 대화 기록을 이 모양으로 조립해 넘긴다(= 경계 어댑터).
 * 이 DTO 자체가 정규화된 입력 계약이다. 실명·연락처 같은 개인식별정보는 담지 않는 것을 전제로 한다.
 */
@Getter
@NoArgsConstructor
public class SummaryRequest {

    @NotEmpty
    @Valid
    private List<Message> messages;

    /** 방 제목 등 맥락 (선택). */
    private String roomTitle;

    @Getter
    @NoArgsConstructor
    public static class Message {
        @NotNull
        private MessageRole role;

        @NotBlank
        private String text;

        /** 발화 시각 (선택). 시간순 정렬·맥락에만 쓰인다. */
        private LocalDateTime sentAt;
    }
}

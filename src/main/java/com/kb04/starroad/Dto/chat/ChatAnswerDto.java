package com.kb04.starroad.Dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAnswerDto {

    private String answer;

    /**
     * 근거 자료를 찾아 답한 경우 true.
     * false 면 검색 단계에서 걸러져 LLM 을 호출하지 않았다는 뜻이다.
     */
    private boolean grounded;

    private List<SourceDto> sources;

    /** 검색은 됐지만 LLM 이 인용번호를 하나도 달지 않은 경우 true (신뢰도 경고용) */
    private boolean uncited;

    /**
     * 근거를 찾지 못했을 때의 응답. LLM 을 호출하지 않는다.
     * (환각 차단 L1 — 검색 게이트)
     */
    public static ChatAnswerDto notFound(String message) {
        return ChatAnswerDto.builder()
                .answer(message)
                .grounded(false)
                .sources(Collections.emptyList())
                .uncited(false)
                .build();
    }
}

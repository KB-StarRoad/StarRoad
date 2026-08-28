package com.kb04.starroad.Controller;

import com.kb04.starroad.Dto.chat.ChatAnswerDto;
import com.kb04.starroad.Dto.chat.ChatAskRequestDto;
import com.kb04.starroad.Service.ChatService;
import com.kb04.starroad.Service.RagIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;
import java.util.Map;

@Tag(name = "AI 상담 챗봇 API")
@RequiredArgsConstructor
@RestController
public class ChatController {

    private final ChatService chatService;
    private final RagIndexService ragIndexService;

    @Operation(summary = "챗봇 화면", description = "AI 상담 챗봇 페이지를 연다")
    @GetMapping("/starroad/chat")
    public ModelAndView chatPage() {
        return new ModelAndView("chat/chat");
    }

    @Operation(summary = "챗봇 질문",
            description = "청년정책·예적금 상품 자료에서 근거를 찾아 답변한다. "
                    + "근거를 찾지 못하면 LLM 을 호출하지 않고 '답변 불가'를 반환한다")
    @PostMapping("/starroad/chat/ask")
    public ChatAnswerDto ask(@RequestBody ChatAskRequestDto request) {
        return chatService.ask(request.getQuestion());
    }

    @Operation(summary = "색인 재생성",
            description = "DB 의 정책·상품을 다시 읽어 벡터 색인을 만든다. 데이터 갱신 후 호출한다")
    @PostMapping("/starroad/chat/reindex")
    public Map<String, Object> reindex() {
        int count = ragIndexService.reindex();
        return Collections.singletonMap("indexed", count);
    }
}

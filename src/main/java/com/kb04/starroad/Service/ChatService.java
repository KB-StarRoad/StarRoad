package com.kb04.starroad.Service;

import com.kb04.starroad.Dto.chat.ChatAnswerDto;
import com.kb04.starroad.Dto.chat.SourceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 기반 상담 챗봇.
 *
 * <p>환각(hallucination)을 막기 위해 4개 층을 둔다.
 * <ol>
 *   <li><b>L1 검색 게이트</b> — 유사도 기준에 걸리는 자료가 없으면 LLM 을 호출하지 않는다.
 *       모델에게 물어보지 않으면 지어낼 기회 자체가 없다.</li>
 *   <li><b>L2 컨텍스트 한정</b> — 시스템 프롬프트로 제공된 자료 밖 지식 사용을 금지한다.</li>
 *   <li><b>L3 인용 검증</b> — 답변의 [n] 인용번호를 파싱해 실제 DB row 와 연결한다.
 *       하나도 인용하지 않았다면 경고 플래그를 세운다.</li>
 *   <li><b>L4 숫자 비위임</b> — 금리·기간은 DB 원본값을 출처 카드에 그대로 실어 보낸다.
 *       LLM 이 쓴 숫자와 화면에서 대조된다.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String QUERY_PREFIX = "query: ";

    private static final Pattern CITATION = Pattern.compile("\\[(\\d+)\\]");

    private static final String NOT_FOUND_MESSAGE =
            "제가 가진 청년정책·예적금 상품 자료로는 답변드릴 수 없습니다. "
                    + "정책명이나 상품명을 좀 더 구체적으로 알려주시면 다시 찾아보겠습니다.";

    private static final String SYSTEM_PROMPT = """
            당신은 청년 자산관리 서비스 '스타로드'의 상담 챗봇이다.

            반드시 지켜야 할 규칙:
            1. 아래 <자료>에 적힌 내용만 근거로 답한다. 자료에 없는 사실은 절대 만들어내지 않는다.
            2. 자료만으로 답할 수 없으면 정확히 이렇게만 답한다:
               "제가 가진 자료로는 답변드릴 수 없습니다."
            3. 문장마다 근거가 된 자료의 번호를 [1], [2] 형식으로 문장 끝에 붙인다.
            4. 금리·기간·금액 등 숫자는 자료에 적힌 값을 그대로 옮긴다. 직접 계산하거나 추정하지 않는다.
            5. 자료에 없는 외부 지식(다른 은행 상품, 일반 상식, 최신 뉴스)은 언급하지 않는다.
            6. 한국어로, 3~5문장 이내로 간결하게 답한다.
            """;

    private final SimpleVectorStore vectorStore;
    private final ChatClient chatClient;

    /** 검색해 올 문서 수 */
    @Value("${starroad.rag.top-k:4}")
    private int topK;

    /**
     * 코사인 유사도 하한. 이 값 미만이면 '관련 자료 없음'으로 처리한다.
     *
     * <p>multilingual-e5 는 무관한 문장끼리도 0.78 안팎이 나오는 특성이 있어
     * 임계값을 낮게 잡으면 게이트가 그대로 무력해진다.
     * {@code RagRetrievalCalibrationTest} 실측값(관련 0.872~0.911 / 무관 0.778~0.797)에 따라
     * 0.83 을 기본값으로 둔다. 데이터가 바뀌면 그 테스트를 다시 돌려 재조정할 것.
     */
    @Value("${starroad.rag.similarity-threshold:0.83}")
    private double similarityThreshold;

    public ChatAnswerDto ask(String question) {
        if (!StringUtils.hasText(question)) {
            return ChatAnswerDto.notFound("질문을 입력해 주세요.");
        }

        // ---------- L1. 검색 게이트 ----------
        List<Document> found = vectorStore.similaritySearch(SearchRequest.builder()
                .query(QUERY_PREFIX + question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build());

        if (found == null || found.isEmpty()) {
            log.debug("[RAG] 임계값 {} 이상 자료 없음 — LLM 호출 생략. 질문: {}", similarityThreshold, question);
            return ChatAnswerDto.notFound(NOT_FOUND_MESSAGE);
        }

        // ---------- L2. 컨텍스트 한정 프롬프트 ----------
        String answer;
        try {
            answer = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(buildUserMessage(question, found))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[RAG] LLM 호출 실패", e);
            return ChatAnswerDto.notFound(
                    "답변 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }

        if (!StringUtils.hasText(answer)) {
            return ChatAnswerDto.notFound(NOT_FOUND_MESSAGE);
        }

        // ---------- L3 + L4. 인용 검증 & DB 원본 수치 첨부 ----------
        Set<Integer> citedNumbers = parseCitations(answer, found.size());
        List<SourceDto> sources = toSources(found, citedNumbers);

        return ChatAnswerDto.builder()
                .answer(answer)
                .grounded(true)
                .sources(sources)
                .uncited(citedNumbers.isEmpty())
                .build();
    }

    /** DB 를 다시 읽어 색인을 만들고 싶을 때 쓰는 진입점은 {@link RagIndexService#reindex()} 이다. */
    private String buildUserMessage(String question, List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        sb.append("<자료>\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            sb.append("[").append(i + 1).append("] ");
            sb.append(stripPassagePrefix(doc.getText())).append("\n\n");
        }

        sb.append("</자료>\n\n");
        sb.append("질문: ").append(question);
        return sb.toString();
    }

    private String stripPassagePrefix(String text) {
        if (text == null) {
            return "";
        }
        return text.startsWith(RagIndexService.PASSAGE_PREFIX)
                ? text.substring(RagIndexService.PASSAGE_PREFIX.length())
                : text;
    }

    /** 답변 본문에서 [n] 을 찾아 유효 범위의 번호만 추린다. */
    private Set<Integer> parseCitations(String answer, int documentCount) {
        Set<Integer> cited = new LinkedHashSet<>();
        Matcher matcher = CITATION.matcher(answer);
        while (matcher.find()) {
            int n = Integer.parseInt(matcher.group(1));
            // 자료에 없는 번호를 지어낸 경우는 버린다.
            if (n >= 1 && n <= documentCount) {
                cited.add(n);
            }
        }
        return cited;
    }

    private List<SourceDto> toSources(List<Document> documents, Set<Integer> citedNumbers) {
        List<SourceDto> sources = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            int citationNo = i + 1;
            sources.add(SourceDto.builder()
                    .citationNo(citationNo)
                    .type(metaString(doc, RagIndexService.META_TYPE))
                    .no(metaInt(doc, RagIndexService.META_NO))
                    .name(metaString(doc, RagIndexService.META_NAME))
                    .link(metaString(doc, RagIndexService.META_LINK))
                    .factLine(metaString(doc, RagIndexService.META_FACT))
                    .score(doc.getScore())
                    .cited(citedNumbers.contains(citationNo))
                    .build());
        }
        return sources;
    }

    private String metaString(Document doc, String key) {
        Object value = doc.getMetadata().get(key);
        return value == null ? "" : value.toString();
    }

    private int metaInt(Document doc, String key) {
        Object value = doc.getMetadata().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

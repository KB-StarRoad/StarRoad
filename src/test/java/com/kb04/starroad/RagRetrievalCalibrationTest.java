package com.kb04.starroad;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 임베딩 모델과 검색 임계값 캘리브레이션.
 *
 * <p>이 테스트의 목적은 통과/실패보다 <b>출력된 유사도 수치를 보고
 * {@code starroad.rag.similarity-threshold} 를 정하는 것</b>이다.
 *
 * <p>임계값은 환각 차단의 1차 방어선이다. 너무 낮으면 무관한 자료가 통과해
 * LLM 이 엉뚱한 근거로 답하고, 너무 높으면 정상 질문에도 "모른다"만 반복한다.
 *
 * <p>최초 실행 시 ONNX 모델(약 470MB)을 내려받으므로 시간이 걸린다.
 */
class RagRetrievalCalibrationTest {

    private static final String MODEL_URI =
            "https://huggingface.co/Xenova/multilingual-e5-small/resolve/main/onnx/model.onnx";
    private static final String TOKENIZER_URI =
            "https://huggingface.co/Xenova/multilingual-e5-small/resolve/main/tokenizer.json";

    private static final String PASSAGE = "passage: ";
    private static final String QUERY = "query: ";

    private static SimpleVectorStore vectorStore;

    @BeforeAll
    static void setUp() throws Exception {
        TransformersEmbeddingModel embeddingModel = new TransformersEmbeddingModel();
        embeddingModel.setModelResource(MODEL_URI);
        embeddingModel.setTokenizerResource(TOKENIZER_URI);
        embeddingModel.setResourceCacheDirectory("./.embedding-model");
        embeddingModel.afterPropertiesSet();

        vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        // 실제 Policy/Product 행을 흉내 낸 표본
        List<Document> docs = new ArrayList<>();
        docs.add(doc("policy-1", "청년 월세 한시 특별지원",
                "정책명: 청년 월세 한시 특별지원\n지역: 서울\n분류: 생활지원\n"
                        + "내용: 만 19~34세 무주택 청년에게 월 최대 20만원의 월세를 최장 12개월 지원한다."));
        docs.add(doc("policy-2", "청년내일저축계좌",
                "정책명: 청년내일저축계좌\n지역: 중앙부처\n분류: 금융자산 형성\n"
                        + "내용: 매월 10만원을 저축하면 정부가 10만원을 추가 적립해 3년 만기 시 목돈을 마련한다."));
        docs.add(doc("product-1", "KB청년희망적금",
                "상품명: KB청년희망적금\n종류: 적금\n분류: 청년우대\n최고금리: 연 5.00%\n"
                        + "가입기간: 12~36개월\n내용: 만 19~34세 청년을 위한 우대금리 적금 상품이다."));
        vectorStore.add(docs);
    }

    private static Document doc(String id, String name, String text) {
        return Document.builder()
                .id(id)
                .text(PASSAGE + text)
                .metadata(Map.of("name", name))
                .build();
    }

    /** 임계값 없이 전 구간 점수를 찍어 관련/무관 질의의 점수 분포를 눈으로 확인한다. */
    @Test
    void printSimilarityDistribution() {
        String[] relevant = {
                "서울 사는 청년 월세 지원 받을 수 있어?",
                "목돈 모으는 정부 지원 정책 알려줘",
                "청년 적금 금리 제일 높은 거 뭐야"
        };
        String[] irrelevant = {
                "오늘 서울 날씨 어때?",
                "주식으로 단타 치는 법 알려줘",
                "파이썬으로 크롤링하는 코드 짜줘"
        };

        System.out.println("\n================ 유사도 분포 (임계값 튜닝용) ================");

        double maxRelevant = 0.0;
        double minRelevant = 1.0;
        System.out.println("\n[관련 있는 질문] — 이 점수들보다 낮게 임계값을 잡아야 통과한다");
        for (String q : relevant) {
            double top = topScore(q);
            minRelevant = Math.min(minRelevant, top);
            maxRelevant = Math.max(maxRelevant, top);
            System.out.printf("  %.4f  %s%n", top, q);
        }

        double maxIrrelevant = 0.0;
        System.out.println("\n[무관한 질문] — 이 점수들보다 높게 임계값을 잡아야 걸러진다");
        for (String q : irrelevant) {
            double top = topScore(q);
            maxIrrelevant = Math.max(maxIrrelevant, top);
            System.out.printf("  %.4f  %s%n", top, q);
        }

        System.out.println("\n----------------------------------------------------------");
        System.out.printf("관련 질문 최저점 : %.4f%n", minRelevant);
        System.out.printf("무관 질문 최고점 : %.4f%n", maxIrrelevant);
        if (minRelevant > maxIrrelevant) {
            System.out.printf("→ 권장 임계값   : %.2f  (두 값의 중간)%n",
                    Math.round((minRelevant + maxIrrelevant) / 2 * 100) / 100.0);
            System.out.println("   application.properties 의 starroad.rag.similarity-threshold 에 반영할 것");
        } else {
            System.out.println("→ 구간이 겹친다. 임계값만으로는 분리가 안 되므로 "
                    + "문서 텍스트 구성이나 임베딩 모델을 재검토해야 한다.");
        }
        System.out.println("==========================================================\n");
    }

    private double topScore(String question) {
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
                .query(QUERY + question)
                .topK(1)
                .similarityThreshold(0.0)
                .build());
        if (results == null || results.isEmpty() || results.get(0).getScore() == null) {
            return 0.0;
        }
        return results.get(0).getScore();
    }

    /** 관련 질문이면 의도한 문서가 1순위로 잡혀야 한다 (한국어 검색이 실제로 되는지 확인). */
    @Test
    void koreanQueryFindsExpectedDocument() {
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
                .query(QUERY + "서울 청년 월세 지원 정책")
                .topK(1)
                .similarityThreshold(0.0)
                .build());

        assertFalse(results == null || results.isEmpty(), "검색 결과가 비어 있다");
        String name = String.valueOf(results.get(0).getMetadata().get("name"));
        System.out.println("[검색 1순위] " + name);
        assertTrue(name.contains("월세"),
                "월세 질문에 월세 정책이 1순위로 나와야 하는데 '" + name + "' 이 나왔다");
    }
}

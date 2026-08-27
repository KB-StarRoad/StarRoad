package com.kb04.starroad.Service;

import com.kb04.starroad.Entity.Policy;
import com.kb04.starroad.Entity.Product;
import com.kb04.starroad.Repository.PolicyRepository;
import com.kb04.starroad.Repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DB 의 정책·상품 데이터를 벡터 색인으로 만드는 서비스.
 *
 * <p>RAG 의 'R'(검색)이 성립하려면 먼저 각 row 를 임베딩해 두어야 한다.
 * 색인은 애플리케이션 기동 시 1회 수행하고 파일로 저장해, 재기동 때는
 * 다시 임베딩하지 않고 불러온다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagIndexService {

    /**
     * multilingual-e5 계열 모델은 학습 시 문서에 "passage: ", 질의에 "query: " 접두사를
     * 붙였다. 동일하게 맞춰야 검색 품질이 나온다.
     */
    public static final String PASSAGE_PREFIX = "passage: ";

    public static final String META_TYPE = "type";
    public static final String META_NO = "no";
    public static final String META_NAME = "name";
    public static final String META_LINK = "link";
    public static final String META_FACT = "fact";

    public static final String TYPE_POLICY = "policy";
    public static final String TYPE_PRODUCT = "product";

    private final SimpleVectorStore vectorStore;
    private final PolicyRepository policyRepository;
    private final ProductRepository productRepository;

    @Value("${starroad.rag.index-file:./rag-index.json}")
    private String indexFilePath;

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        File indexFile = new File(indexFilePath);
        try {
            if (indexFile.exists()) {
                vectorStore.load(indexFile);
                log.info("[RAG] 기존 색인을 불러왔다: {}", indexFile.getAbsolutePath());
            } else {
                reindex();
            }
        } catch (Exception e) {
            // DB 가 떠 있지 않거나 임베딩 모델 다운로드에 실패해도 앱 자체는 기동시킨다.
            // 이 경우 챗봇만 "자료 없음"으로 동작한다.
            log.warn("[RAG] 색인 초기화 실패 — 챗봇이 답변할 자료가 없는 상태다. 원인: {}", e.toString());
        }
    }

    /**
     * DB 를 다시 읽어 색인을 통째로 만든다. 정책·상품 데이터를 갱신한 뒤 호출한다.
     *
     * @return 색인된 문서 수
     */
    public int reindex() {
        List<Document> documents = new ArrayList<>();

        for (Policy policy : policyRepository.findAll()) {
            documents.add(toDocument(policy));
        }
        for (Product product : productRepository.findAll()) {
            documents.add(toDocument(product));
        }

        if (documents.isEmpty()) {
            log.warn("[RAG] 색인할 정책·상품 데이터가 DB 에 없다.");
            return 0;
        }

        vectorStore.add(documents);

        File indexFile = new File(indexFilePath);
        vectorStore.save(indexFile);
        log.info("[RAG] 문서 {}건 색인 완료 → {}", documents.size(), indexFile.getAbsolutePath());

        return documents.size();
    }

    private Document toDocument(Policy policy) {
        String endDate = policy.getEndDate() == null
                ? "상시"
                : new SimpleDateFormat("yyyy-MM-dd").format(policy.getEndDate());

        String fact = String.format("지역: %s · 분류: %s · 마감: %s",
                policy.getLocation(), policy.getTag(), endDate);

        // 임베딩 대상 텍스트. 정책명·지역·태그를 함께 넣어야 "서울 청년 월세" 같은
        // 복합 질의가 본문에만 의존하지 않고 걸린다.
        String text = String.format(
                "정책명: %s%n지역: %s%n분류: %s%n마감일: %s%n내용: %s",
                policy.getName(), policy.getLocation(), policy.getTag(), endDate, policy.getExplain());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_TYPE, TYPE_POLICY);
        metadata.put(META_NO, policy.getNo());
        metadata.put(META_NAME, policy.getName());
        metadata.put(META_LINK, policy.getLink());
        metadata.put(META_FACT, fact);

        return Document.builder()
                .id(TYPE_POLICY + "-" + policy.getNo())
                .text(PASSAGE_PREFIX + text)
                .metadata(metadata)
                .build();
    }

    private Document toDocument(Product product) {
        String kind = (product.getType() != null && product.getType() == 'S') ? "적금" : "예금";

        StringBuilder fact = new StringBuilder();
        fact.append(String.format("%s · 최고 연 %.2f%%", kind, product.getMaxRate()));
        fact.append(String.format(" · %d~%d개월", product.getMinPeriod(), product.getMaxPeriod()));
        fact.append(String.format(" · 최소 %,d원", product.getMinPrice()));
        if (product.getMaxPrice() != null) {
            fact.append(String.format(" · 최대 %,d원", product.getMaxPrice()));
        }

        String text = String.format(
                "상품명: %s%n종류: %s%n분류: %s%n최고금리: 연 %.2f%%%n가입기간: %d~%d개월%n"
                        + "가입금액: 최소 %,d원%n내용: %s",
                product.getName(), kind, product.getAttribute(), product.getMaxRate(),
                product.getMinPeriod(), product.getMaxPeriod(), product.getMinPrice(),
                product.getExplain());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_TYPE, TYPE_PRODUCT);
        metadata.put(META_NO, product.getNo());
        metadata.put(META_NAME, product.getName());
        metadata.put(META_LINK, product.getLink());
        metadata.put(META_FACT, fact.toString());

        return Document.builder()
                .id(TYPE_PRODUCT + "-" + product.getNo())
                .text(PASSAGE_PREFIX + text)
                .metadata(metadata)
                .build();
    }
}

package com.kb04.starroad.Dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 챗봇 답변의 근거가 된 자료 1건.
 *
 * <p>여기 담기는 값은 모두 DB 원본이다. LLM 이 생성한 텍스트가 아니므로,
 * 화면에서 답변 본문과 나란히 보여주면 사용자가 숫자를 직접 대조할 수 있다.
 * (환각 차단 L4 — 숫자를 LLM 에 위임하지 않는다)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDto {

    /** 답변 본문의 [n] 인용번호와 대응된다 */
    private int citationNo;

    /** "policy" 또는 "product" */
    private String type;

    /** 원본 테이블의 PK */
    private int no;

    private String name;

    /** 원문 상세 페이지 URL */
    private String link;

    /** 검색 시 계산된 코사인 유사도. 튜닝·디버깅용 */
    private Double score;

    /** 화면에 그대로 노출할 DB 원본 수치 요약 (예: "최고 연 5.0% · 12~36개월") */
    private String factLine;

    /** LLM 이 답변에서 실제로 인용한 자료인지 여부 */
    private boolean cited;
}

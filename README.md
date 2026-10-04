# Starroad 💫
💸 청년들의 효과적인 자산 운용을 위한 웹 서비스

<img width="1280" alt="starroadscreenshot" src="https://github.com/KB-StarRoad/.github/assets/98302932/ac6f42fe-1693-419f-bc52-cf6cdf697dde">


<br/>
<br/>


<br/>

## 🐳 프로젝트 개요 
정부 혹은 지자체에서 청년층에게 자산을 마련하거나 경제적으로 도움을 주는 여러 청년 정책을 시행하고 있습니다. 하지만 청년층은 청년 정책에 대해 잘 알지 못할 뿐만 아니라 본인에게 필요한 정책을 쉽게 알 수 없다고 합니다. 또한, 정부에서 지원하는 청년 희망 적금 서비스를 중도 해지한 사람이 청년층 4명 중 1명 꼴이라고 합니다. 

즉, 현재 청년층은 다양한 청년 정책을 효율적으로 이용하지 못하고 예적금 만기까지 달성하지 못하는 비율이 적지 않음을 알 수 있습니다. 

따라서 청년층에게 필요한 청년 정책을 쉽게 찾을 수 있고 현재 본인의 자산을 기반으로 적합한 금융 상품을 추천하고 지속할 수 있도록 돕는 서비스, **스타로드**를 기획하게 되었습니다. 


<br/>

## 🐳 기술 스택

| 구분 | 기술 |
|---|---|
| 언어·빌드 | Java 21, Maven (WAR) |
| 서버 | Spring Boot 3.5.16, Spring MVC, Spring Data JPA, Spring Security |
| 화면 | JSP · JSTL, JavaScript(fetch), jQuery, Bootstrap 5, Chart.js |
| DB | Oracle (운영), H2 인메모리 (dev 프로필) |
| AI | Spring AI 1.1.8 — 생성 모델 Anthropic Claude · Google Gemini · Ollama(로컬) 중 선택 |
| 임베딩·검색 | JVM 내장 ONNX(multilingual-e5-small), SimpleVectorStore |
| API 문서·관측 | springdoc-openapi 2.8.17 (Swagger UI), Spring Boot Actuator |
| 테스트 | JUnit 5 (spring-boot-starter-test) |

<br/>

## 🐳 주요 기능
### 🍿 KB 예적금 상품 추천
국민 은행 내의 금융 상품 중 사용자의 우대 조건을 포함한 최대 이율 및 월 수입과 저금 목표치를 고려하여 상품 만기 시 받을 수 있는 최대 금액을 계산하여 제공합니다. 

같은 상품이라도 충족한 우대 조건이 달라 실제로 받는 이율은 사람마다 다릅니다. 계산은 `MaturityCalculator`가 세 단계로 합니다.

| 단계 | 계산 |
|---|---|
| 적용 이율 | 기본 금리(최고 금리 − 최대 우대금리, 기간을 고르면 그 기간의 기본 금리) + **회원이 충족한 우대금리**. 최고 금리를 넘지 않습니다. |
| 가입 기간 | 회원이 고른 기간(상품 최장 기간 이내). 고르지 않으면 최고 금리가 적용되는 기간. |
| 세후 수령액 | 월 납입액 × n + 세전 이자 − 세금. 세전 이자 = 월 납입액 × n(n+1)/2 × 연 이율 / 12 (단리). 일반과세 15.4% 또는 비과세. |

월 납입액은 `월 수입 × 저축 목표(%) − 이미 납입 중인 적금`입니다. 
예: 최고 연 5.00%(우대 1.20%p 포함) 적금에 월 30만원을 24개월 넣으면, 우대 조건을 못 채운 회원은 연 3.80%로 7,441,110원, 
0.50%p를 채운 회원은 연 4.30%로 7,472,835원을 받습니다. 계산 규칙은 `MaturityCalculatorTest`, `ProductEstimateTest`로 검증합니다.

### 🍿 자산 차트 및 적금 챌린지
현재까지 KB국민은행과 거래한 내역을 토대로 구성한 자산 차트를 제공합니다. 매달 적금 납입 시 제공하는 별을 모두 모으면 리워드(포인트리)를 지급합니다. 
또한, 커뮤니티에서 사람들과 금융 정보를 공유하고 챌린지를 독려합니다.

### 🍿 한눈에 보는 청년 금융 정책
관심 있는 정책을 '즐겨찾기'할 수 있습니다. 
관심 정책의 마감 날짜가 임박 시 홈 화면에서 알림을 제공합니다.

### 🍿 AI 정책 상담 챗봇 (RAG)
등록된 청년정책·예적금 상품 데이터에서 근거를 찾아 답변하는 챗봇입니다.

<br/>

## 🐳 시연 영상 
**회원 가입**

![part1](https://github.com/KB-StarRoad/.github/assets/98302932/d4a27a32-4db7-4a0b-85ae-f52c9ebf63a5)

<br/>

**청년 금융 정책**

![part2](https://github.com/KB-StarRoad/.github/assets/98302932/2b7783d4-c973-4e5e-8572-6dee6acf80dd)

<br/>

**KB 예적금 상품 추천 및 자산 차트 및 적금 챌린지**

![part3](https://github.com/KB-StarRoad/.github/assets/98302932/9a4bff6e-5369-4d81-9a57-c04751ea0496)

<br/>

**커뮤니티**

![part4](https://github.com/KB-StarRoad/.github/assets/98302932/127c1d04-cb73-4928-93c0-e3b78a15cb81)

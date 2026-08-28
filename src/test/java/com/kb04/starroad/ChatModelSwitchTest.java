package com.kb04.starroad;

import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 생성 모델 스위치({@code spring.ai.model.chat}) 검증.
 *
 * <p><b>왜 필요한가.</b> 채팅 모델 스타터를 셋(Anthropic·Ollama·Google GenAI) 넣어 두었는데,
 * Spring AI 의 자동설정은 모두 {@code matchIfMissing = true} 다. 즉 스위치를 비워 두면
 * 셋이 동시에 활성화돼 ChatModel 빈이 여러 개 생기고, RagConfig 의
 * {@code chatClient(ChatModel)} 주입이 NoUniqueBeanDefinition 으로 터진다.
 *
 * <p>전체 컨텍스트를 띄우면 임베딩 ONNX 로딩까지 따라와 느리므로,
 * ApplicationContextRunner 로 AI 자동설정만 얹어 빠르게 확인한다.
 * Ollama 데몬이나 API 키가 없어도 빈 생성 자체는 이뤄지므로 검증이 가능하다.
 */
class ChatModelSwitchTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    // 채팅 모델들이 의존하는 보조 자동설정
                    RestClientAutoConfiguration.class,
                    SpringAiRetryAutoConfiguration.class,
                    ToolCallingAutoConfiguration.class,
                    OllamaApiAutoConfiguration.class,
                    // 검증 대상
                    AnthropicChatAutoConfiguration.class,
                    OllamaChatAutoConfiguration.class,
                    GoogleGenAiChatAutoConfiguration.class))
            // 기동 시 모델을 내려받으려 들지 않게 한다 (Ollama 데몬이 없는 환경 대비)
            .withPropertyValues("spring.ai.ollama.init.pull-model-strategy=never");

    @Test
    void anthropicSwitchGivesExactlyOneChatModel() {
        runner.withPropertyValues(
                        "spring.ai.model.chat=anthropic",
                        "spring.ai.anthropic.api-key=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatModel.class);
                    assertThat(context).hasSingleBean(AnthropicChatModel.class);
                });
    }

    @Test
    void ollamaSwitchGivesExactlyOneChatModel() {
        runner.withPropertyValues("spring.ai.model.chat=ollama")
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatModel.class);
                    assertThat(context).hasSingleBean(OllamaChatModel.class);
                });
    }

    @Test
    void googleGenAiSwitchGivesExactlyOneChatModel() {
        runner.withPropertyValues(
                        "spring.ai.model.chat=google-genai",
                        "spring.ai.google.genai.api-key=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatModel.class);
                    assertThat(context).hasSingleBean(GoogleGenAiChatModel.class);
                });
    }

    /**
     * 스위치를 비워 두면 셋이 전부 뜬다 — 이게 바로 application.properties 에서
     * spring.ai.model.chat 을 반드시 명시해야 하는 이유다. 그 전제가 깨지면
     * (예: Spring AI 가 기본 동작을 바꾸면) 이 테스트가 알려 준다.
     */
    @Test
    void missingSwitchActivatesMultipleChatModels() {
        runner.withPropertyValues(
                        "spring.ai.anthropic.api-key=test",
                        "spring.ai.google.genai.api-key=test")
                .run(context -> assertThat(context.getBeansOfType(ChatModel.class))
                        .as("스위치를 비우면 자동설정이 중복 활성화된다")
                        .hasSizeGreaterThan(1));
    }
}

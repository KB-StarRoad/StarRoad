package com.kb04.starroad.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 설정.
 * swagger-ui 는 /swagger-ui.html, API 명세는 /v3/api-docs 로 노출된다.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI starroadOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("STARROAD")
                        .description("스타로드 API 문서")
                        .version("1.0"));
    }
}

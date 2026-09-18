package com.lifehub.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lifehubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lifehub API")
                        .description("개인용 종합 관리 웹앱 API (가계부 / 할일·캘린더 / 취업준비 현황)")
                        .version("v0.1"));
    }
}

package com.zenalyst.factory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Factory Machine Coverage API - Zenalyst AI Backend")
                        .version("1.0.0")
                        .description("Automates the supervisor's morning shift crisis across 18 machines: " +
                                "balancing mandatory certifications, overtime budget caps, and preventing the chronic burnout of willing workers.")
                        .contact(new Contact()
                                .name("Zenalyst AI Backend Team")
                                .email("engineering@zenalyst.ai"))
                        .license(new License().name("Apache 2.0")));
    }
}

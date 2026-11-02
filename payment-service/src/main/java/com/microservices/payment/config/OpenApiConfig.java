package com.microservices.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .description("Microservice for processing payments, wallet transactions, and refunds.")
                        .version("v1.0.0")
                        .contact(new Contact().name("Engineering Team").email("engineering@example.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}

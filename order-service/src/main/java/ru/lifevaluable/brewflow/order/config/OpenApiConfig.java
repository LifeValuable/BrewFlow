package ru.lifevaluable.brewflow.order.config;

import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI brewFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BrewFlow Order Service API")
                        .description("API для просмотра каталога, работы с корзиной и заказами в кофейном интернет-магазине")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                )
                .tags(Arrays.asList(
                        new Tag().name("Products").description("Каталог товаров"),
                        new Tag().name("Cart").description("Управление корзиной"),
                        new Tag().name("Orders").description("Управление заказами"),
                        new Tag().name("Internal").description("Внутренние API для микросервисов")
                ));
    }
}


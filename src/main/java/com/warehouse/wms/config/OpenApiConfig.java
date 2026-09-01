package com.warehouse.wms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI warehouseOpenApi() {
        return new OpenAPI().info(new Info().title("WMS REST API").version("v1")
                .description("Warehouse inventory, location and outbound-picking operations.")
                .license(new License().name("Private")));
    }
}

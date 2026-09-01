package com.warehouse.wms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        // SPA routing for non-API client-side URLs without file extensions
                        if (!resourcePath.startsWith("api/") && !resourcePath.startsWith("v3/") && !resourcePath.startsWith("swagger-ui/")) {

                            Resource index = location.createRelative("index.html");
                            if (index.exists() && index.isReadable()) {
                                return index;
                            }
                        }
                        return null;
                    }
                });
    }
}

package com.example.aiticketservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerUiRedirectConfig implements WebMvcConfigurer {

    private static final String SWAGGER_UI_REDIRECT =
            "redirect:/webjars/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config";

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/swagger-ui.html").setViewName(SWAGGER_UI_REDIRECT);
        registry.addViewController("/swagger-ui/index.html").setViewName(SWAGGER_UI_REDIRECT);
    }
}

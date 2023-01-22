package io.roach.pipeline.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebMvc
@Configuration
@ApplicationProfiles.Online
public class WebConfiguration implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!registry.hasMappingForPattern("/webjars/**")) {
            registry.addResourceHandler("/webjars/**")
                    .addResourceLocations("classpath:/META-INF/resources/webjars/");
        }
        if (!registry.hasMappingForPattern("/**")) {
            registry.addResourceHandler("/**")
                    .addResourceLocations(
                            "classpath:/META-INF/resources/",
                            "classpath:/resources/",
                            "classpath:/static/"
                    );
        }
        if (!registry.hasMappingForPattern("/browser/**")) {
            registry.addResourceHandler("/browser/**").addResourceLocations(
                    "/webjars/hal-explorer/1.2.0/");
        }
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(
                MediaTypes.HAL_FORMS_JSON,
                MediaTypes.HAL_JSON,
                MediaTypes.VND_ERROR_JSON,
                MediaType.APPLICATION_JSON,
                MediaType.ALL);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new FormHttpMessageConverter());
    }
}

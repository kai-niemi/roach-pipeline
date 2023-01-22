package io.roach.pipeline.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

import io.roach.pipeline.web.filter.SLF4JRequestLoggingFilter;

@Configuration
@ApplicationProfiles.Online
public class WebFilterConfiguration {
    @Bean
    public SLF4JRequestLoggingFilter slf4JLoggingFilter() {
        SLF4JRequestLoggingFilter filter = new SLF4JRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setIncludeHeaders(true);
        filter.setMaxPayloadLength(10000);
        return filter;
    }

    @Bean
    public FilterRegistrationBean<SLF4JRequestLoggingFilter> loggingFilterRegistration() {
        FilterRegistrationBean<SLF4JRequestLoggingFilter> registrationBean
                = new FilterRegistrationBean<>();

        registrationBean.setFilter(slf4JLoggingFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);

        return registrationBean;
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

/*
    @Bean
    public GzipRequestFilter gzipRequestFilter() {
        return new GzipRequestFilter();
    }

    @Bean
    public FilterRegistrationBean<GzipRequestFilter> gzipRequestFilterRegistrationBean() {
        FilterRegistrationBean<GzipRequestFilter> registrationBean
                = new FilterRegistrationBean<>();

        registrationBean.setFilter(gzipRequestFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(2);

        return registrationBean;
    }

    @Bean
    public GzipResponseFilter gzipResponseFilter() {
        return new GzipResponseFilter();
    }

    @Bean
    public FilterRegistrationBean<GzipResponseFilter> gzipResponseFilterFilterRegistrationBean() {
        FilterRegistrationBean<GzipResponseFilter> registrationBean
                = new FilterRegistrationBean<>();

        registrationBean.setFilter(gzipResponseFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(3);

        return registrationBean;
    }
*/
}

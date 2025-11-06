package com.bank.iolog.config;

import com.bank.iolog.filter.RequestWrappingFilter;
import com.bank.iolog.service.IOLoggerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class IOLoggerFilterConfig {

    @Value("${spring.application.name:unknown-service}")
    private String sourceApplication;

    @Bean
    public FilterRegistrationBean<RequestWrappingFilter> requestWrappingFilter(IOLoggerService ioLoggerService) {
        FilterRegistrationBean<RequestWrappingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestWrappingFilter(ioLoggerService, sourceApplication));
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE); // Ensure it runs before most filters
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("ioLoggerRequestWrappingFilter");
        return registrationBean;
    }
}

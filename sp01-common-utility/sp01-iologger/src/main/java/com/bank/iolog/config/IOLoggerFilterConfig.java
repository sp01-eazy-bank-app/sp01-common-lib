package com.bank.iolog.config;

import com.bank.iolog.filter.RequestWrappingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IOLoggerFilterConfig {

    @Bean
    public FilterRegistrationBean<RequestWrappingFilter> requestWrappingFilter() {
        FilterRegistrationBean<RequestWrappingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestWrappingFilter());
        registrationBean.setOrder(1); // Ensure it runs early
        return registrationBean;
    }
}

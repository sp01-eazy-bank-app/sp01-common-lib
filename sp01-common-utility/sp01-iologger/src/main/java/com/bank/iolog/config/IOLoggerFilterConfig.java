package com.bank.iolog.config;

import com.bank.iolog.filter.RequestWrappingFilter;
import com.bank.iolog.repository.IOLogEntryRepository;
import com.bank.iolog.service.IOLoggerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.DependsOn;

@Configuration
@DependsOn("ioLoggerTransactionManager")
@ConditionalOnProperty(prefix = "iologger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IOLoggerFilterConfig {

    @Value("${spring.application.name:unknown-service}")
    private String sourceApplication;

    @Bean
    public IOLoggerService ioLoggerService(IOLogEntryRepository ioLogEntryRepository) {
        return new IOLoggerService(ioLogEntryRepository);
    }

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

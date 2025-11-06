package com.bank.iolog.config;

import com.bank.iolog.aspect.IOLoggerAspect;
import com.bank.iolog.repository.IOLogEntryRepository;
import com.bank.iolog.service.IOLoggerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@DependsOn("ioLoggerTransactionManager")
public class IOLoggerAutoConfiguration {

    @Bean(name = "ioLoggerService")
    public IOLoggerService ioLoggerService(IOLogEntryRepository ioLogEntryRepository) {
        return new IOLoggerService(ioLogEntryRepository);
    }
    //====================

    @Bean
    public IOLoggerAspect ioLoggerAspect(IOLoggerService ioLoggerService) {
        return new IOLoggerAspect(ioLoggerService);
    }
}

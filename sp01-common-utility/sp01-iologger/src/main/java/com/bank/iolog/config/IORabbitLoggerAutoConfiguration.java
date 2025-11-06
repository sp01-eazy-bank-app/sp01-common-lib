package com.bank.iolog.config;

import com.bank.iolog.aspect.RabbitInboundLoggerAspect;
import com.bank.iolog.aspect.RabbitOutboundLoggerAspect;
import com.bank.iolog.service.IOLoggerService;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
@DependsOn("ioLoggerService")
public class IORabbitLoggerAutoConfiguration {

    @Value("${spring.application.name:unknown-service}")
    private String appName;

    @Bean
    public RabbitOutboundLoggerAspect rabbitOutboundLoggerAspect(IOLoggerService ioLoggerService) {
        return new RabbitOutboundLoggerAspect(ioLoggerService);
    }

    @Bean
    public RabbitInboundLoggerAspect rabbitInboundLoggerAspect(IOLoggerService ioLoggerService) {
        return new RabbitInboundLoggerAspect(ioLoggerService, appName);
    }

    @Bean
    @ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            RabbitInboundLoggerAspect rabbitInboundLoggerAspect) {

        // Create the listener container factory
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        // Apply Spring Boot’s default settings (like concurrency, prefetch, etc.)
        configurer.configure(factory, connectionFactory);

        // Attach your inbound logging advice to the listener factory
        factory.setAdviceChain(new Advice[]{ rabbitInboundLoggerAspect });

        return factory;
    }
}

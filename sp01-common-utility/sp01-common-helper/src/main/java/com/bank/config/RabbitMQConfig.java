package com.bank.config;

import com.bank.iolog.util.IOLoggerConstant;
import com.bank.iolog.util.IOLoggerUtil;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setBeforePublishPostProcessors((Message message) -> {
            MessageProperties props = message.getMessageProperties();
            // --- TRACE ID ---
            String traceId = Optional.ofNullable(MDC.get(IOLoggerConstant.TRACE_ID))
                    .orElseGet(IOLoggerUtil::generateTraceId);
            props.setHeader(IOLoggerConstant.TRACE_ID, traceId);
            return message;
        });
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}

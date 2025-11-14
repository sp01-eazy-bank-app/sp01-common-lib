package com.bank.helper.util;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitHelper {

    private final RabbitTemplate rabbitTemplate;

    private static final String CORRELATION_ID_HEADER = "correlation_id";

    /*
    Send a message to the DEFAULT exchange with routing key equal to queue name without correlationId
     */
    public void sendMessage(String queueName, Object message) {
        rabbitTemplate.convertAndSend(queueName, message);
    }

    /*
    Send a message to the DEFAULT exchange with routing key equal to queue name with a correlationId
     */
    public void sendMessage(String queueName, Object message, String correlationId) {
        rabbitTemplate.convertAndSend(queueName, message, msg -> {
            msg.getMessageProperties().setCorrelationId(correlationId);
            msg.getMessageProperties().setHeader(CORRELATION_ID_HEADER, correlationId);
            return msg;
        });
    }

    /*
    Send a message to either DIRECT or TOPIC exchange with a routing key without correlationId
     */
    public void sendMessage(String exchange, String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    /*
    Send a message to either DIRECT or TOPIC exchange with a routing key and correlation ID.
     */
    public void sendMessage(String exchange, String routingKey, Object message, String correlationId) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
            msg.getMessageProperties().setCorrelationId(correlationId);
            msg.getMessageProperties().setHeader(CORRELATION_ID_HEADER, correlationId);
            return msg;
        });
    }

    /*
    Send a message to FANOUT exchange (no routing key) without correlationId
     */
    public void publishEvent(String exchange, Object message) {
        rabbitTemplate.convertAndSend(exchange, "", message);
    }

    /*
    Send a message to FANOUT exchange (no routing key) with correlationId
     */
    public void publishEvent(String exchange, Object message, String correlationId) {
        rabbitTemplate.convertAndSend(exchange, "", message, msg -> {
            msg.getMessageProperties().setCorrelationId(correlationId);
            msg.getMessageProperties().setHeader(CORRELATION_ID_HEADER, correlationId);
            return msg;
        });
    }
}

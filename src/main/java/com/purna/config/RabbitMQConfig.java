package com.purna.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQConfig
 * 
 * WHY USE THIS:
 * The user requested an advanced genuine RabbitMQ linkage for maximum decoupling! 
 * We define exact Routing boundaries here. Payments go into the "amazon.payment.exchange", 
 * where they map securely through the "payment.success" routing key and land into the 
 * "amazon.payment.queue" until the Java worker spins up and processes them!
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "amazon.payment.exchange.v3";
    public static final String QUEUE_NAME = "amazon.payment.queue.v3";
    public static final String ROUTING_KEY = "payment.success";

    public static final String DLX_EXCHANGE_NAME = "amazon.payment.dlx";
    public static final String DLQ_QUEUE_NAME = "amazon.payment.dlq";

    @Bean
    public Queue paymentQueue() {
        // WHY USE THIS:
        // Attaches Dead Letter logic! If a message fails processing (NACK'd), instead of crashing 
        // the broker in an endless loop, it redirects to our DLQ for manual inspection later!
        return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY + ".dlq")
                .build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE_NAME);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ_QUEUE_NAME, true);
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(ROUTING_KEY + ".dlq");
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue paymentQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentQueue).to(paymentExchange).with(ROUTING_KEY);
    }

    // Converts POJOs seamlessly to JSON Strings for the AMQP tunnel
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

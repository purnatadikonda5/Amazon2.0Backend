package com.purna.event;

import com.purna.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * PaymentRabbitListener (The Worker Node)
 * 
 * WHY USE THIS:
 * This listens aggressively to the actual CloudAMQP WebSocket for data streaming down!
 * You no longer need @Async because Spring automatically allocates an independent 
 * `SimpleMessageListenerContainer` thread pool just to process these chunks.
 * We achieved perfect remote hardware scaling decoupling.
 */
@Slf4j
@Component
@lombok.RequiredArgsConstructor
public class PaymentRabbitListener {

    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event, com.rabbitmq.client.Channel channel, org.springframework.messaging.Message<?> message) throws java.io.IOException {
        log.info("🔥 RABBITMQ WORKER TRIGGERED: Starting robust tasks for Order ID: {} from the CloudAMQP Server!", event.getOrderId());
        
        long deliveryTag = (long) message.getHeaders().get(org.springframework.amqp.support.AmqpHeaders.DELIVERY_TAG);
        
        // ⚡ ADVANCED RABBITMQ: Idempotency Check using Redis
        // Network glitches can cause RabbitMQ to resend a message. We strictly check 
        // Redis memory to see if we already dispatched the invoice!
        String idempotencyKey = "processed_order:" + event.getOrderId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "true", java.time.Duration.ofHours(24));
        
        if (Boolean.FALSE.equals(isNew)) {
            log.warn("⚠️ Idempotency Triggered! Order {} was already processed. Silently acknowledging duplicate.", event.getOrderId());
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            // Emulate Heavy Backend Service logic without stalling the API Gateway Thread
            Thread.sleep(5000);
            log.info("📧 RABBITMQ WORKER SUCCESS: Dispatched elaborate PDF Invoice to Buyer Email: {} for INR {}", 
                     event.getBuyerEmail(), event.getAmount());
            channel.basicAck(deliveryTag, false);
                     
        } catch (InterruptedException e) {
            log.error("CloudAMQP AMQP-Worker crashed while generating invoices.");
            Thread.currentThread().interrupt();
            redisTemplate.delete(idempotencyKey); // Allow retry to happen
            // requeue = true for temporary infrastructure glitch
            channel.basicNack(deliveryTag, false, true);
        } catch (Exception e) {
            log.error("Unexpected error in AMQP-Worker. Routing to DLQ...", e);
            redisTemplate.delete(idempotencyKey); 
            // requeue = false routes this poisoned message directly into our Dead Letter Queue!
            channel.basicNack(deliveryTag, false, false); 
        }
    }
}

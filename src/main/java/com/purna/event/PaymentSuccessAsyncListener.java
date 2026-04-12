package com.purna.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * PaymentSuccessAsyncListener (The Worker Node)
 * 
 * WHY USE THIS:
 * This entirely simulates RabbitMQ / Apache Kafka Decoupling. 
 * The @Async tag pushes this method out of the main Tomcat HTTP thread into a detached Java 
 * Background Worker Pool.
 * 
 * If mimicking sending a bulky PDF Invoice email takes 6 full seconds, the Buyer's Web browser 
 * doesn't lag 6 seconds waiting! The checkout returns instantly, and this worker hums along quietly 
 * in the background until the email is fired. "Eventual Consistency" is achieved.
 */
@Slf4j
@Component
public class PaymentSuccessAsyncListener {

    @Async // Crucial requirement for threading offloading!
    @EventListener
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
        log.info("ASYNC WORKER DEPLOYED: Starting heavy background tasks for Order ID: {}", event.getOrderId());
        
        try {
            // Emulate 5 second massive delay mapping to generating a PDF Invoice / Mailing Client
            Thread.sleep(5000);
            log.info("📧 ASYNC WORKER SUCCESS: Dispatched elaborate PDF Invoice to Buyer Email: {} for INR {}", 
                     event.getBuyerEmail(), event.getAmount());
                     
        } catch (InterruptedException e) {
            log.error("Async Worker crashed while generating invoices.");
            Thread.currentThread().interrupt();
        }
    }
}

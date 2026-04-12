package com.purna.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AsyncEventConfig (The Message Broker Equivalent)
 * 
 * WHY USE THIS:
 * In a real-world company (like Amazon), Webhooks must return HTTP 200 SUCCESS in under 3 seconds.
 * If generating an invoice PDF or sending an email takes 5 seconds, the entire Payment thread stalls, 
 * causing the Gateway to think it failed!
 * 
 * @EnableAsync creates a background worker ThreadPool. Instead of executing heavy logic in the main 
 * HTTP thread, we push an Event onto the queue and instantly return 200 OK. The background workers 
 * pick it up ensuring "Eventual Consistency". (This flawlessly mimics RabbitMQ/Kafka architecture).
 */
@Configuration
@EnableAsync // Turns on the internal Message Queue worker pool
public class AsyncEventConfig {
}

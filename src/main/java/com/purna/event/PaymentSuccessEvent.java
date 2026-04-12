package com.purna.event;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PaymentSuccessEvent
 * 
 * WHY USE THIS:
 * This acts as the exact JSON packet we shoot across the CloudAMQP server over TCP. 
 * Notice it doesn't extend ApplicationEvent anymore because it's no longer just isolated to 
 * the JVM memory! It's traveling through a real external Enterprise Message broker.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent implements Serializable {
    
    // ⚡ ADVANCED RABBITMQ: Message Versioning
    // By tracking version=2, if the consumer updates logic tomorrow, we don't break backwards compatibility!
    private int version = 2;
    private String type = "ORDER_CREATED";
    
    private Long orderId;
    private String buyerEmail;
    private Double amount;
}

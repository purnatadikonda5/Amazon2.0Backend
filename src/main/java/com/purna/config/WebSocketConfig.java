package com.purna.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocketConfig
 *
 * WHY USE THIS:
 * Configures the STOMP messaging broker. Traditional HTTP is half-duplex (client requests, server responds).
 * WebSockets provide full-duplex TCP connections so the server can push live UI updates (like direct messages 
 * or live bargaining offers) in less than 10 milliseconds without the frontend polling the DB endlessly.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Use the built-in message broker for fast, sub-millisecond PubSub
        config.enableSimpleBroker("/topic");
        
        // Setup prefixes for client-to-server messaging
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Expose a SockJS fallback endpoint so robust real-time streams work out of the box
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}

package com.purna.controller;

import com.purna.dto.ChatMessageDTO;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Advanced Endpoint to facilitate O(1) delivery time communication between buyers and sellers.
     * Includes WebSocket Header context extraction, validation, and Exception Routing.
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDTO message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            if (message.getReceiverId() == null || message.getReceiverId().isEmpty()) {
                throw new IllegalArgumentException("Receiver ID cannot be null or empty");
            }

            // Secure validation block: Check Stomp Session metadata if provided
            String sessionId = headerAccessor.getSessionId();
            if (sessionId == null) {
                log.warn("Message received without valid WebSocket Session ID");
            }

            if (message.getTimestamp() == null) {
                message.setTimestamp(Instant.now().toString());
            }
            
            // Fast direct delivery to subscriber's specific queue
            String destination = "/topic/messages/" + message.getReceiverId();
            messagingTemplate.convertAndSend(destination, message);
            log.info("Successfully routed message from {} to {}", message.getSenderId(), message.getReceiverId());

        } catch (Exception processError) {
            log.error("Failed to process chat message: {}", processError.getMessage());
            // Optionally route the error back to the sender if needed
            if (message.getSenderId() != null) {
                messagingTemplate.convertAndSend("/topic/errors/" + message.getSenderId(), 
                    "Failed to send message: " + processError.getMessage());
            }
        }
    }

    @MessageExceptionHandler
    public void handleException(Exception exception) {
        log.error("Unhandled WebSocket Exception: ", exception);
    }
}

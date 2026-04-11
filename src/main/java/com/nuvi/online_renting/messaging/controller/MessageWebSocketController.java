package com.nuvi.online_renting.messaging.controller;

import com.nuvi.online_renting.messaging.dto.MessageResponseDTO;
import com.nuvi.online_renting.messaging.dto.SendMessageRequestDTO;
import com.nuvi.online_renting.messaging.service.MessageService;
import com.nuvi.online_renting.users.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket message controller — handles STOMP messages from clients.
 *
 * Client sends to  : /app/booking/{bookingId}/send  (body: { "content": "..." })
 * Server pushes to : /topic/booking/{bookingId}/messages
 *
 * The incoming message is persisted via the existing MessageService so the
 * REST API and WebSocket share the same data path.
 */
@Controller
public class MessageWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(MessageWebSocketController.class);

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageWebSocketController(MessageService messageService,
                                      SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/booking/{bookingId}/send")
    public void sendMessage(@DestinationVariable Long bookingId,
                            WsMessagePayload payload,
                            Principal principal) {
        if (principal == null) {
            log.warn("Unauthenticated WebSocket send attempt on booking {}", bookingId);
            return;
        }

        User sender = resolveUser(principal);
        if (sender == null) return;

        SendMessageRequestDTO dto = new SendMessageRequestDTO();
        dto.setBookingId(bookingId);
        dto.setContent(payload.getContent());

        MessageResponseDTO saved = messageService.sendMessage(dto);

        // Broadcast the saved message to all subscribers of this booking's topic
        messagingTemplate.convertAndSend("/topic/booking/" + bookingId + "/messages", saved);
        log.debug("WebSocket message broadcast: booking={}, sender={}", bookingId, sender.getEmail());
    }

    private User resolveUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    // ─── Inner payload DTO ────────────────────────────────────────────────────

    public static class WsMessagePayload {
        private String content;
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}

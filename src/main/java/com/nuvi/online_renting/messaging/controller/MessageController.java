package com.nuvi.online_renting.messaging.controller;

import com.nuvi.online_renting.messaging.dto.MessageResponseDTO;
import com.nuvi.online_renting.messaging.dto.SendMessageRequestDTO;
import com.nuvi.online_renting.messaging.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Messaging", description = "Booking-scoped messaging between renter and seller.")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @Operation(
            summary = "Send a message",
            description = "Send a message within a booking conversation. Only the renter and seller of the booking can participate."
    )
    @PostMapping
    @PreAuthorize("hasAnyAuthority('SEND_MESSAGE', 'FULL_ACCESS')")
    public ResponseEntity<MessageResponseDTO> sendMessage(@Valid @RequestBody SendMessageRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.sendMessage(request));
    }

    @Operation(
            summary = "Get conversation for a booking",
            description = "Returns all messages for the given booking, ordered oldest-first. Also marks all unread messages addressed to the caller as read."
    )
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyAuthority('SEND_MESSAGE', 'FULL_ACCESS')")
    public ResponseEntity<List<MessageResponseDTO>> getConversation(@PathVariable Long bookingId) {
        return ResponseEntity.ok(messageService.getConversation(bookingId));
    }

    @Operation(
            summary = "Mark a single message as read",
            description = "Marks the specified message as read. Only the recipient of the message can call this."
    )
    @PatchMapping("/{messageId}/read")
    @PreAuthorize("hasAnyAuthority('SEND_MESSAGE', 'FULL_ACCESS')")
    public ResponseEntity<MessageResponseDTO> markAsRead(@PathVariable Long messageId) {
        return ResponseEntity.ok(messageService.markAsRead(messageId));
    }

    @Operation(
            summary = "Get total unread message count",
            description = "Returns the number of unread messages addressed to the currently authenticated user across all conversations."
    )
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyAuthority('SEND_MESSAGE', 'FULL_ACCESS')")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", messageService.getUnreadCount()));
    }
}

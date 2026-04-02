package com.nuvi.online_renting.messaging.service;

import com.nuvi.online_renting.messaging.dto.MessageResponseDTO;
import com.nuvi.online_renting.messaging.dto.SendMessageRequestDTO;

import java.util.List;

public interface MessageService {

    MessageResponseDTO sendMessage(SendMessageRequestDTO request);

    List<MessageResponseDTO> getConversation(Long bookingId);

    MessageResponseDTO markAsRead(Long messageId);

    long getUnreadCount();
}

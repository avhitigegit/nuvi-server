package com.nuvi.online_renting.messaging.serviceImpl;

import com.nuvi.online_renting.bookings.model.Booking;
import com.nuvi.online_renting.bookings.repository.BookingRepository;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.messaging.dto.MessageResponseDTO;
import com.nuvi.online_renting.messaging.dto.SendMessageRequestDTO;
import com.nuvi.online_renting.messaging.model.Message;
import com.nuvi.online_renting.messaging.repository.MessageRepository;
import com.nuvi.online_renting.messaging.service.MessageService;
import com.nuvi.online_renting.users.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final BookingRepository bookingRepository;
    private final AuthenticationFacade authFacade;

    public MessageServiceImpl(MessageRepository messageRepository,
                              BookingRepository bookingRepository,
                              AuthenticationFacade authFacade) {
        this.messageRepository = messageRepository;
        this.bookingRepository = bookingRepository;
        this.authFacade = authFacade;
    }

    @Override
    @Transactional
    public MessageResponseDTO sendMessage(SendMessageRequestDTO request) {
        User currentUser = authFacade.getCurrentUser();

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + request.getBookingId()));

        User renter = booking.getUser();
        User seller = booking.getItem().getSeller();

        User recipient;
        if (currentUser.getId().equals(renter.getId())) {
            recipient = seller;
        } else if (currentUser.getId().equals(seller.getId())) {
            recipient = renter;
        } else {
            throw new ForbiddenException("You are not a participant in this booking's conversation");
        }

        Message message = new Message();
        message.setBooking(booking);
        message.setSender(currentUser);
        message.setRecipient(recipient);
        message.setContent(request.getContent());

        return toDTO(messageRepository.save(message));
    }

    @Override
    @Transactional
    public List<MessageResponseDTO> getConversation(Long bookingId) {
        User currentUser = authFacade.getCurrentUser();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));

        validateParticipant(currentUser, booking);

        // Mark all messages sent to this user in this conversation as read
        messageRepository.markConversationAsRead(bookingId, currentUser.getId(), LocalDateTime.now());

        return messageRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public MessageResponseDTO markAsRead(Long messageId) {
        User currentUser = authFacade.getCurrentUser();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id " + messageId));

        if (!message.getRecipient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only mark your own received messages as read");
        }

        if (message.getReadAt() == null) {
            message.setReadAt(LocalDateTime.now());
            message = messageRepository.save(message);
        }

        return toDTO(message);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User currentUser = authFacade.getCurrentUser();
        return messageRepository.countUnreadByRecipientId(currentUser.getId());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validateParticipant(User user, Booking booking) {
        Long renterId = booking.getUser().getId();
        Long sellerId = booking.getItem().getSeller().getId();
        if (!user.getId().equals(renterId) && !user.getId().equals(sellerId)) {
            throw new ForbiddenException("You are not a participant in this booking's conversation");
        }
    }

    private MessageResponseDTO toDTO(Message m) {
        MessageResponseDTO dto = new MessageResponseDTO();
        dto.setId(m.getId());
        dto.setBookingId(m.getBooking().getId());
        dto.setSenderId(m.getSender().getId());
        dto.setSenderName(m.getSender().getName());
        dto.setRecipientId(m.getRecipient().getId());
        dto.setRecipientName(m.getRecipient().getName());
        dto.setContent(m.getContent());
        dto.setReadAt(m.getReadAt());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }
}

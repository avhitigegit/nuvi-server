package com.nuvi.online_renting.messaging.repository;

import com.nuvi.online_renting.messaging.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    // Count unread messages where the current user is the recipient
    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient.id = :userId AND m.readAt IS NULL")
    long countUnreadByRecipientId(@Param("userId") Long userId);

    // Mark all unread messages in a conversation as read
    @Modifying
    @Query("UPDATE Message m SET m.readAt = :now WHERE m.booking.id = :bookingId AND m.recipient.id = :userId AND m.readAt IS NULL")
    void markConversationAsRead(@Param("bookingId") Long bookingId,
                                @Param("userId") Long userId,
                                @Param("now") LocalDateTime now);
}

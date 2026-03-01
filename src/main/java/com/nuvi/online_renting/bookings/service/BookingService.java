package com.nuvi.online_renting.bookings.service;

import com.nuvi.online_renting.bookings.dto.BookingRequestDTO;
import com.nuvi.online_renting.bookings.dto.BookingResponseDTO;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.BookingStatus;
import org.springframework.data.domain.Pageable;

public interface BookingService {

    BookingResponseDTO createBooking(BookingRequestDTO bookingRequestDTO);

    BookingResponseDTO getBookingById(Long id);

    PagedResponse<BookingResponseDTO> getAllBookings(String status, Long userId, Pageable pageable);

    BookingResponseDTO updateBooking(Long id, BookingRequestDTO bookingRequestDTO);

    void deleteBooking(Long id);

    BookingResponseDTO updateStatus(Long id, BookingStatus bookingStatus);
}

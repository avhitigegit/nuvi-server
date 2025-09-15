package com.nuvi.online_renting.bookings.service;

import com.nuvi.online_renting.bookings.dto.BookingRequestDTO;
import com.nuvi.online_renting.bookings.dto.BookingResponseDTO;
import com.nuvi.online_renting.common.dto.BookingStatus;

import java.util.List;

public interface BookingService {

    BookingResponseDTO createBooking(BookingRequestDTO bookingRequestDTO);

    BookingResponseDTO getBookingById(Long id);

    List<BookingResponseDTO> getAllBookings();

    BookingResponseDTO updateBooking(Long id, BookingRequestDTO bookingRequestDTO);

    void deleteBooking(Long id);

    BookingResponseDTO updateStatus(Long id, BookingStatus bookingStatus);


}

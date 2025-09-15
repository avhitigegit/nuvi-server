package com.nuvi.online_renting.bookings.controller;

import com.nuvi.online_renting.bookings.dto.BookingRequestDTO;
import com.nuvi.online_renting.bookings.dto.BookingResponseDTO;
import com.nuvi.online_renting.bookings.service.BookingService;
import com.nuvi.online_renting.common.dto.BookingStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody @Valid BookingRequestDTO bookingRequestDTO, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookingService.createBooking(bookingRequestDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> updateBooking(@PathVariable Long id,
                                                            @RequestBody BookingRequestDTO bookingRequestDTO) {
        return ResponseEntity.ok(bookingService.updateBooking(id, bookingRequestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam BookingStatus bookingStatus) {

        BookingResponseDTO response = bookingService.updateStatus(id, bookingStatus);
        return ResponseEntity.ok(response);
    }
}
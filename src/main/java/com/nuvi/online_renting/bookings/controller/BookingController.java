package com.nuvi.online_renting.bookings.controller;

import com.nuvi.online_renting.bookings.dto.BookingRequestDTO;
import com.nuvi.online_renting.bookings.dto.BookingResponseDTO;
import com.nuvi.online_renting.bookings.service.BookingService;
import com.nuvi.online_renting.common.enums.BookingStatus;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final AuthenticationFacade authFacade;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_BOOKING')")
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody @Valid BookingRequestDTO bookingRequestDTO) {
        Long userId = authFacade.getCurrentUser().getId();
        bookingRequestDTO.setUserId(userId);
        return ResponseEntity.ok(bookingService.createBooking(bookingRequestDTO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_BOOKINGS', 'VIEW_ALL_BOOKINGS')")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_BOOKINGS', 'VIEW_ALL_BOOKINGS')")
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CREATE_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<BookingResponseDTO> updateBooking(@PathVariable Long id,
                                                            @RequestBody BookingRequestDTO bookingRequestDTO) {
        return ResponseEntity.ok(bookingService.updateBooking(id, bookingRequestDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CANCEL_OWN_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('UPDATE_BOOKING_STATUS')")
    public ResponseEntity<BookingResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam BookingStatus bookingStatus) {
        return ResponseEntity.ok(bookingService.updateStatus(id, bookingStatus));
    }
}

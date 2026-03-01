package com.nuvi.online_renting.bookings.controller;

import com.nuvi.online_renting.bookings.dto.BookingRequestDTO;
import com.nuvi.online_renting.bookings.dto.BookingResponseDTO;
import com.nuvi.online_renting.bookings.service.BookingService;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.BookingStatus;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final AuthenticationFacade authFacade;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_BOOKING')")
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody @Valid BookingRequestDTO dto) {
        dto.setUserId(authFacade.getCurrentUser().getId());
        return ResponseEntity.ok(bookingService.createBooking(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_BOOKINGS', 'VIEW_ALL_BOOKINGS')")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    // GET /api/bookings?status=PENDING&userId=3&page=0&size=10&sort=startDate,asc
    @GetMapping
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_BOOKINGS', 'VIEW_ALL_BOOKINGS')")
    public ResponseEntity<PagedResponse<BookingResponseDTO>> getAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(bookingService.getAllBookings(status, userId, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CREATE_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<BookingResponseDTO> updateBooking(@PathVariable Long id,
                                                            @RequestBody BookingRequestDTO dto) {
        return ResponseEntity.ok(bookingService.updateBooking(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CANCEL_OWN_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('UPDATE_BOOKING_STATUS')")
    public ResponseEntity<BookingResponseDTO> updateStatus(@PathVariable Long id,
                                                           @RequestParam BookingStatus bookingStatus) {
        return ResponseEntity.ok(bookingService.updateStatus(id, bookingStatus));
    }
}

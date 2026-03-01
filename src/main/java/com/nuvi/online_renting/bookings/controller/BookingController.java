package com.nuvi.online_renting.bookings.controller;

import com.nuvi.online_renting.bookings.dto.BookingRequestDTO;
import com.nuvi.online_renting.bookings.dto.BookingResponseDTO;
import com.nuvi.online_renting.bookings.service.BookingService;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.BookingStatus;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Bookings", description = "Manage the full rental booking lifecycle — from creation and confirmation through to completion and return tracking.")
public class BookingController {

    private final BookingService bookingService;
    private final AuthenticationFacade authFacade;

    @Operation(
            summary = "Create a new booking",
            description = "A logged-in USER or SELLER books an item for a specified date range. " +
                          "The system automatically checks item availability and prevents overlapping bookings. " +
                          "The booking starts in PENDING status and must be confirmed by an ADMIN."
    )
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_BOOKING')")
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody @Valid BookingRequestDTO dto) {
        dto.setUserId(authFacade.getCurrentUser().getId());
        return ResponseEntity.ok(bookingService.createBooking(dto));
    }

    @Operation(summary = "Get booking by ID", description = "Retrieve details of a single booking by its ID. Users can view their own bookings; ADMINs can view any booking.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_BOOKINGS', 'VIEW_ALL_BOOKINGS')")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @Operation(
            summary = "List and filter bookings",
            description = "Returns a paginated list of bookings. Filter by status (PENDING, CONFIRMED, CANCELLED, COMPLETED) and/or userId. " +
                          "ADMINs can see all bookings; regular users see only their own. " +
                          "Example: GET /api/bookings?status=PENDING&userId=3&page=0&size=10&sort=startDate,asc"
    )
    @GetMapping
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_BOOKINGS', 'VIEW_ALL_BOOKINGS')")
    public ResponseEntity<PagedResponse<BookingResponseDTO>> getAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(bookingService.getAllBookings(status, userId, pageable));
    }

    @Operation(summary = "Update a booking", description = "Update the item or date range of an existing booking. Only bookings in PENDING status can be updated. The booking owner or ADMIN can perform this action.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CREATE_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<BookingResponseDTO> updateBooking(@PathVariable Long id,
                                                            @RequestBody BookingRequestDTO dto) {
        return ResponseEntity.ok(bookingService.updateBooking(id, dto));
    }

    @Operation(summary = "Delete a booking", description = "Permanently removes a booking record. This is a hard delete. To cancel a booking while keeping the record, use PATCH /{id}/status with status=CANCELLED instead.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CANCEL_OWN_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update booking status (Admin)",
            description = "Admin-only action to change the status of a booking. " +
                          "Allowed transitions: PENDING → CONFIRMED, PENDING → CANCELLED, CONFIRMED → CANCELLED. " +
                          "To mark a booking as COMPLETED (item returned), use PATCH /{id}/complete instead."
    )
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('UPDATE_BOOKING_STATUS')")
    public ResponseEntity<BookingResponseDTO> updateStatus(@PathVariable Long id,
                                                           @RequestParam BookingStatus bookingStatus) {
        return ResponseEntity.ok(bookingService.updateStatus(id, bookingStatus));
    }

    @Operation(
            summary = "Complete a booking — item returned (Admin)",
            description = "Admin marks a CONFIRMED booking as COMPLETED, meaning the renter has returned the item. " +
                          "An optional return note (e.g. condition of the item) can be added. " +
                          "The item's availability is automatically restored to true after this action. " +
                          "Example: PATCH /api/bookings/5/complete?returnNote=Returned+in+good+condition"
    )
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('UPDATE_BOOKING_STATUS')")
    public ResponseEntity<BookingResponseDTO> completeBooking(@PathVariable Long id,
                                                              @RequestParam(required = false) String returnNote) {
        return ResponseEntity.ok(bookingService.completeBooking(id, returnNote));
    }

    @Operation(
            summary = "Get active bookings for an item (Seller / Admin)",
            description = "Returns all PENDING and CONFIRMED bookings for a specific item. " +
                          "Useful for sellers to see who has booked their item and when. " +
                          "Cancelled and completed bookings are excluded. " +
                          "Example: GET /api/bookings/item/3?page=0&size=10"
    )
    @GetMapping("/item/{itemId}")
    @PreAuthorize("hasAnyAuthority('UPDATE_OWN_ITEM', 'FULL_ACCESS')")
    public ResponseEntity<PagedResponse<BookingResponseDTO>> getActiveBookingsByItem(
            @PathVariable Long itemId,
            @PageableDefault(size = 10, sort = "startDate") Pageable pageable) {
        return ResponseEntity.ok(bookingService.getActiveBookingsByItem(itemId, pageable));
    }
}

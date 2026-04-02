package com.nuvi.online_renting.recurring.controller;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.recurring.dto.RecurringBookingRequestDTO;
import com.nuvi.online_renting.recurring.dto.RecurringBookingResponseDTO;
import com.nuvi.online_renting.recurring.service.RecurringBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recurring-bookings")
@Tag(name = "Recurring Bookings", description = "Create and manage recurring / subscription rental templates.")
public class RecurringBookingController {

    private final RecurringBookingService recurringBookingService;

    public RecurringBookingController(RecurringBookingService recurringBookingService) {
        this.recurringBookingService = recurringBookingService;
    }

    @Operation(
            summary = "Create a recurring booking template",
            description = "Creates the template and immediately generates the first booking occurrence (status: PENDING). " +
                          "Subsequent occurrences are created automatically by the daily scheduler."
    )
    @PostMapping
    @PreAuthorize("hasAnyAuthority('CREATE_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<RecurringBookingResponseDTO> createTemplate(
            @Valid @RequestBody RecurringBookingRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurringBookingService.createTemplate(request));
    }

    @Operation(summary = "List my recurring booking templates")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CREATE_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<PagedResponse<RecurringBookingResponseDTO>> getMyTemplates(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(recurringBookingService.getMyTemplates(pageable));
    }

    @Operation(summary = "Get a recurring booking template by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CREATE_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<RecurringBookingResponseDTO> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(recurringBookingService.getTemplate(id));
    }

    @Operation(summary = "Pause a recurring booking template", description = "The scheduler will not create new occurrences while PAUSED.")
    @PatchMapping("/{id}/pause")
    @PreAuthorize("hasAnyAuthority('CREATE_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<RecurringBookingResponseDTO> pauseTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(recurringBookingService.pauseTemplate(id));
    }

    @Operation(summary = "Resume a paused recurring booking template")
    @PatchMapping("/{id}/resume")
    @PreAuthorize("hasAnyAuthority('CREATE_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<RecurringBookingResponseDTO> resumeTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(recurringBookingService.resumeTemplate(id));
    }

    @Operation(summary = "Cancel a recurring booking template", description = "Stops all future occurrences. Already-created bookings are unaffected.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CREATE_BOOKING', 'FULL_ACCESS')")
    public ResponseEntity<RecurringBookingResponseDTO> cancelTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(recurringBookingService.cancelTemplate(id));
    }
}

package com.nuvi.online_renting.disputes.serviceImpl;

import com.nuvi.online_renting.auth.service.EmailService;
import com.nuvi.online_renting.bookings.model.Booking;
import com.nuvi.online_renting.bookings.repository.BookingRepository;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.BookingStatus;
import com.nuvi.online_renting.common.enums.DisputeStatus;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ConflictException;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.disputes.dto.DisputeRequestDTO;
import com.nuvi.online_renting.disputes.dto.DisputeResolveDTO;
import com.nuvi.online_renting.disputes.dto.DisputeResponseDTO;
import com.nuvi.online_renting.disputes.model.Dispute;
import com.nuvi.online_renting.disputes.repository.DisputeRepository;
import com.nuvi.online_renting.disputes.service.DisputeService;
import com.nuvi.online_renting.users.model.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeServiceImpl.class);

    private final DisputeRepository disputeRepository;
    private final BookingRepository bookingRepository;
    private final AuthenticationFacade authFacade;
    private final EmailService emailService;

    @Override
    @Transactional
    public DisputeResponseDTO raiseDispute(DisputeRequestDTO dto) {
        User currentUser = authFacade.getCurrentUser();

        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + dto.getBookingId()));

        // Only the renter who made the booking can raise a dispute
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only raise a dispute for your own bookings");
        }

        // Dispute only allowed on CONFIRMED or COMPLETED bookings
        BookingStatus bookingStatus = BookingStatus.valueOf(booking.getStatus());
        if (bookingStatus != BookingStatus.CONFIRMED && bookingStatus != BookingStatus.COMPLETED) {
            throw new BadRequestException("Disputes can only be raised for CONFIRMED or COMPLETED bookings");
        }

        // Prevent duplicate disputes for the same booking
        if (disputeRepository.existsByBookingIdAndRaisedById(booking.getId(), currentUser.getId())) {
            throw new ConflictException("You have already raised a dispute for this booking");
        }

        Dispute dispute = new Dispute();
        dispute.setBooking(booking);
        dispute.setRaisedBy(currentUser);
        dispute.setReason(dto.getReason());
        dispute.setDescription(dto.getDescription());
        dispute.setStatus(DisputeStatus.OPEN);

        DisputeResponseDTO result = toDTO(disputeRepository.save(dispute));
        log.info("Dispute {} raised by user {} for booking {}", result.getId(), currentUser.getId(), dto.getBookingId());

        // Notify the seller
        User seller = booking.getItem().getSeller();
        emailService.sendDisputeRaisedToSeller(
                seller.getEmail(), seller.getName(),
                currentUser.getName(), booking.getItem().getName(),
                dto.getReason().name());

        return result;
    }

    @Override
    public DisputeResponseDTO getDisputeById(Long id) {
        Dispute dispute = findDispute(id);
        User currentUser = authFacade.getCurrentUser();

        if (currentUser.getRole() != Role.ADMIN &&
                !dispute.getRaisedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not allowed to view this dispute");
        }

        return toDTO(dispute);
    }

    @Override
    public PagedResponse<DisputeResponseDTO> getAllDisputes(DisputeStatus status, Pageable pageable) {
        User currentUser = authFacade.getCurrentUser();

        // Non-admin users see only their own disputes
        Long userId = currentUser.getRole() == Role.ADMIN ? null : currentUser.getId();

        Page<Dispute> page = disputeRepository.filterDisputes(status, userId, pageable);
        return new PagedResponse<>(page.map(this::toDTO));
    }

    @Override
    @Transactional
    public DisputeResponseDTO markUnderReview(Long id) {
        Dispute dispute = findDispute(id);

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new BadRequestException("Only OPEN disputes can be moved to UNDER_REVIEW");
        }

        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        log.info("Dispute {} moved to UNDER_REVIEW", id);
        return toDTO(disputeRepository.save(dispute));
    }

    @Override
    @Transactional
    public DisputeResponseDTO resolveDispute(Long id, DisputeResolveDTO dto) {
        Dispute dispute = findDispute(id);

        if (dispute.getStatus() != DisputeStatus.OPEN && dispute.getStatus() != DisputeStatus.UNDER_REVIEW) {
            throw new BadRequestException("Only OPEN or UNDER_REVIEW disputes can be resolved");
        }

        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolutionNote(dto.getResolutionNote());
        dispute.setResolvedAt(LocalDateTime.now());

        log.info("Dispute {} resolved by admin", id);
        DisputeResponseDTO resolved = toDTO(disputeRepository.save(dispute));

        User renter = dispute.getRaisedBy();
        emailService.sendDisputeResolvedToRenter(
                renter.getEmail(), renter.getName(),
                dispute.getBooking().getItem().getName(),
                dto.getResolutionNote());

        return resolved;
    }

    @Override
    @Transactional
    public DisputeResponseDTO rejectDispute(Long id, DisputeResolveDTO dto) {
        Dispute dispute = findDispute(id);

        if (dispute.getStatus() != DisputeStatus.OPEN && dispute.getStatus() != DisputeStatus.UNDER_REVIEW) {
            throw new BadRequestException("Only OPEN or UNDER_REVIEW disputes can be rejected");
        }

        dispute.setStatus(DisputeStatus.REJECTED);
        dispute.setResolutionNote(dto.getResolutionNote());
        dispute.setResolvedAt(LocalDateTime.now());

        log.info("Dispute {} rejected by admin", id);
        DisputeResponseDTO rejected = toDTO(disputeRepository.save(dispute));

        User renter = dispute.getRaisedBy();
        emailService.sendDisputeRejectedToRenter(
                renter.getEmail(), renter.getName(),
                dispute.getBooking().getItem().getName(),
                dto.getResolutionNote());

        return rejected;
    }

    private Dispute findDispute(Long id) {
        return disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found with id " + id));
    }

    private DisputeResponseDTO toDTO(Dispute dispute) {
        DisputeResponseDTO dto = new DisputeResponseDTO();
        dto.setId(dispute.getId());
        dto.setBookingId(dispute.getBooking().getId());
        dto.setRaisedByUserId(dispute.getRaisedBy().getId());
        dto.setRaisedByUserName(dispute.getRaisedBy().getName());
        dto.setReason(dispute.getReason());
        dto.setDescription(dispute.getDescription());
        dto.setStatus(dispute.getStatus());
        dto.setResolutionNote(dispute.getResolutionNote());
        dto.setResolvedAt(dispute.getResolvedAt());
        dto.setCreatedAt(dispute.getCreatedAt());
        dto.setUpdatedAt(dispute.getUpdatedAt());
        return dto;
    }
}

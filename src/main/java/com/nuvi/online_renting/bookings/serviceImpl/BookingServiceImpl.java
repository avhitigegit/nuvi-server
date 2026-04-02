package com.nuvi.online_renting.bookings.serviceImpl;

import com.nuvi.online_renting.auth.service.EmailService;
import com.nuvi.online_renting.bookings.dto.BookingRequestDTO;
import com.nuvi.online_renting.coupons.model.Coupon;
import com.nuvi.online_renting.coupons.serviceImpl.CouponServiceImpl;
import com.nuvi.online_renting.item.repository.ItemBlockedDateRepository;
import com.nuvi.online_renting.bookings.dto.BookingResponseDTO;
import com.nuvi.online_renting.bookings.model.Booking;
import com.nuvi.online_renting.bookings.repository.BookingRepository;
import com.nuvi.online_renting.bookings.service.BookingService;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.BookingStatus;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ConflictException;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.item.model.Item;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemBlockedDateRepository blockedDateRepository;
    private final AuthenticationFacade authFacade;
    private final EmailService emailService;
    private final CouponServiceImpl couponService;

    @Override
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + dto.getUserId()));
        // Pessimistic lock — prevents two concurrent requests from booking the same item simultaneously
        Item item = itemRepository.findByIdWithLock(dto.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + dto.getItemId()));

        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        if (item.getSeller().isSuspended()) {
            throw new BadRequestException("This item cannot be booked because the seller's account has been suspended");
        }

        if (!item.isAvailable()) {
            throw new BadRequestException("Item is not available for booking");
        }

        if (bookingRepository.existsOverlappingBooking(item.getId(), dto.getStartDate(), dto.getEndDate())) {
            throw new ConflictException("Item is already booked for the selected date range");
        }

        if (blockedDateRepository.existsOverlappingBlock(item.getId(), dto.getStartDate(), dto.getEndDate())) {
            throw new ConflictException("The item is not available for the selected dates — the seller has blocked this period");
        }

        // Calculate base amount: pricePerDay × number of rental days
        long rentalDays = dto.getEndDate().toEpochDay() - dto.getStartDate().toEpochDay();
        double baseAmount = item.getPricePerDay() * rentalDays;

        // Apply coupon if provided
        double discountAmount = 0.0;
        String appliedCouponCode = null;
        if (dto.getCouponCode() != null && !dto.getCouponCode().isBlank()) {
            Coupon coupon = couponService.findAndValidateCoupon(dto.getCouponCode(), baseAmount);
            discountAmount = couponService.calculateDiscount(coupon, baseAmount);
            appliedCouponCode = coupon.getCode();
            // Increment usage count
            coupon.setUsedCount(coupon.getUsedCount() + 1);
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setItem(item);
        booking.setStartDate(dto.getStartDate());
        booking.setEndDate(dto.getEndDate());
        booking.setStatus(BookingStatus.PENDING.name());
        booking.setTermsAcceptedAt(LocalDateTime.now());
        booking.setTermsAcceptedIp(dto.getClientIp());
        booking.setAppliedCouponCode(appliedCouponCode);
        booking.setDiscountAmount(round2(discountAmount));
        booking.setTotalAmount(round2(Math.max(0, baseAmount - discountAmount)));

        BookingResponseDTO result = convertToDTO(bookingRepository.save(booking));
        log.info("Booking {} created by user {} for item {}", result.getId(), dto.getUserId(), dto.getItemId());

        // Notify the seller about the new booking request
        User seller = item.getSeller();
        emailService.sendBookingCreatedToSeller(
                seller.getEmail(), seller.getName(),
                user.getName(), item.getName(),
                booking.getStartDate().toString(), booking.getEndDate().toString());

        return result;
    }

    @Override
    public BookingResponseDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));

        // GAP 3: Ownership check — user can only view their own booking; admin can view any
        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = booking.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to view this booking");
        }

        return convertToDTO(booking);
    }

    @Override
    public PagedResponse<BookingResponseDTO> getAllBookings(String status, Long userId, Pageable pageable) {
        User currentUser = authFacade.getCurrentUser();

        // Non-admin users can only see their own bookings regardless of userId param
        if (currentUser.getRole() != Role.ADMIN) {
            userId = currentUser.getId();
        }

        Page<Booking> page = bookingRepository.filterBookings(status, userId, pageable);
        return new PagedResponse<>(page.map(this::convertToDTO));
    }

    @Override
    @Transactional
    public BookingResponseDTO updateBooking(Long id, BookingRequestDTO dto) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = booking.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to update this booking");
        }

        if (!BookingStatus.PENDING.name().equals(booking.getStatus())) {
            throw new BadRequestException("Only PENDING bookings can be updated");
        }

        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + dto.getItemId()));

        booking.setItem(item);
        booking.setStartDate(dto.getStartDate());
        booking.setEndDate(dto.getEndDate());

        return convertToDTO(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = booking.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to delete this booking");
        }

        booking.setDeleted(true);
        booking.setDeletedAt(java.time.LocalDateTime.now());
        bookingRepository.save(booking);
        log.info("Booking {} soft-deleted by user {}", id, currentUser.getEmail());
    }

    @Override
    @Transactional
    public BookingResponseDTO updateStatus(Long bookingId, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));

        BookingStatus current = BookingStatus.valueOf(booking.getStatus());

        boolean allowed =
                (current == BookingStatus.PENDING   && newStatus == BookingStatus.CONFIRMED) ||
                (current == BookingStatus.PENDING   && newStatus == BookingStatus.CANCELLED) ||
                (current == BookingStatus.CONFIRMED && newStatus == BookingStatus.CANCELLED);

        if (!allowed) {
            throw new BadRequestException(
                    "Invalid status transition: " + current.name() + " → " + newStatus.name() +
                    ". Allowed: PENDING→CONFIRMED, PENDING→CANCELLED, CONFIRMED→CANCELLED"
            );
        }

        booking.setStatus(newStatus.name());

        // Keep item availability in sync with booking status
        Item item = booking.getItem();
        if (newStatus == BookingStatus.CONFIRMED) {
            item.setAvailable(false);
            itemRepository.save(item);
        } else if (newStatus == BookingStatus.CANCELLED) {
            item.setAvailable(true);
            itemRepository.save(item);
        }

        log.info("Booking {} status changed: {} → {}", bookingId, current.name(), newStatus.name());
        BookingResponseDTO result = convertToDTO(bookingRepository.save(booking));

        User renter = booking.getUser();
        User seller = booking.getItem().getSeller();
        String itemName = booking.getItem().getName();

        if (newStatus == BookingStatus.CANCELLED) {
            emailService.sendBookingCancelledToRenter(renter.getEmail(), renter.getName(), itemName);
            emailService.sendBookingCancelledToSeller(seller.getEmail(), seller.getName(), renter.getName(), itemName);
        }

        return result;
    }

    @Override
    @Transactional
    public BookingResponseDTO completeBooking(Long bookingId, String returnNote) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));

        if (!BookingStatus.CONFIRMED.name().equals(booking.getStatus())) {
            throw new BadRequestException("Only CONFIRMED bookings can be marked as completed");
        }

        booking.setStatus(BookingStatus.COMPLETED.name());
        booking.setReturnedAt(LocalDateTime.now());
        booking.setReturnNote(returnNote);

        Item item = booking.getItem();
        item.setAvailable(true);
        itemRepository.save(item);

        BookingResponseDTO completed = convertToDTO(bookingRepository.save(booking));

        User renter = booking.getUser();
        User seller = booking.getItem().getSeller();
        String completedItemName = booking.getItem().getName();
        emailService.sendBookingCompletedToRenter(renter.getEmail(), renter.getName(), completedItemName);
        emailService.sendBookingCompletedToSeller(seller.getEmail(), seller.getName(), renter.getName(), completedItemName);

        return completed;
    }

    @Override
    public PagedResponse<BookingResponseDTO> getActiveBookingsByItem(Long itemId, Pageable pageable) {
        Page<Booking> page = bookingRepository.findActiveByItemId(itemId, pageable);
        return new PagedResponse<>(page.map(this::convertToDTO));
    }

    @Override
    @Transactional
    public BookingResponseDTO cancelBooking(Long id, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin    = currentUser.getRole() == Role.ADMIN;
        boolean isRenter   = booking.getUser().getId().equals(currentUser.getId());
        boolean isItemOwner = booking.getItem().getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isRenter && !isItemOwner) {
            throw new ForbiddenException("You are not allowed to cancel this booking");
        }

        BookingStatus current = BookingStatus.valueOf(booking.getStatus());
        if (current != BookingStatus.PENDING && current != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only PENDING or CONFIRMED bookings can be cancelled. Current status: " + current);
        }

        booking.setStatus(BookingStatus.CANCELLED.name());
        booking.setCancellationReason(reason);
        booking.setCancelledBy(currentUser.getEmail());
        booking.setCancelledAt(LocalDateTime.now());

        // Restore item availability when a confirmed booking is cancelled
        if (current == BookingStatus.CONFIRMED) {
            booking.getItem().setAvailable(true);
            itemRepository.save(booking.getItem());
        }

        log.info("Booking {} cancelled by {} ({})", id, currentUser.getEmail(),
                isRenter ? "renter" : isItemOwner ? "seller" : "admin");

        BookingResponseDTO result = convertToDTO(bookingRepository.save(booking));

        // Notify the other party
        User renter = booking.getUser();
        User seller = booking.getItem().getSeller();
        String itemName = booking.getItem().getName();
        if (isRenter) {
            emailService.sendBookingCancelledToSeller(seller.getEmail(), seller.getName(), renter.getName(), itemName);
        } else {
            emailService.sendBookingCancelledToRenter(renter.getEmail(), renter.getName(), itemName);
        }

        return result;
    }

    @Override
    @Transactional
    public BookingResponseDTO confirmBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isItemOwner = booking.getItem().getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isItemOwner) {
            throw new ForbiddenException("You can only confirm bookings for your own items");
        }

        if (!BookingStatus.PENDING.name().equals(booking.getStatus())) {
            throw new BadRequestException("Only PENDING bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED.name());
        booking.getItem().setAvailable(false);
        itemRepository.save(booking.getItem());

        log.info("Booking {} confirmed by seller/admin {}", id, currentUser.getEmail());
        BookingResponseDTO confirmed = convertToDTO(bookingRepository.save(booking));

        User renter = booking.getUser();
        emailService.sendBookingConfirmedToRenter(
                renter.getEmail(), renter.getName(),
                booking.getItem().getName(),
                booking.getStartDate().toString(), booking.getEndDate().toString());

        return confirmed;
    }

    @Override
    @Transactional
    public BookingResponseDTO rejectBooking(Long id, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isItemOwner = booking.getItem().getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isItemOwner) {
            throw new ForbiddenException("You can only reject bookings for your own items");
        }

        if (!BookingStatus.PENDING.name().equals(booking.getStatus())) {
            throw new BadRequestException("Only PENDING bookings can be rejected");
        }

        booking.setStatus(BookingStatus.CANCELLED.name());
        booking.setCancellationReason(reason);

        log.info("Booking {} rejected by seller/admin {} — reason: {}", id, currentUser.getEmail(), reason);
        BookingResponseDTO rejected = convertToDTO(bookingRepository.save(booking));

        User renter = booking.getUser();
        emailService.sendBookingRejectedToRenter(
                renter.getEmail(), renter.getName(),
                booking.getItem().getName(), reason);

        return rejected;
    }

    private BookingResponseDTO convertToDTO(Booking booking) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setUserName(booking.getUser().getName());
        dto.setItemId(booking.getItem().getId());
        dto.setItemName(booking.getItem().getName());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setStatus(booking.getStatus());
        dto.setTermsAcceptedAt(booking.getTermsAcceptedAt());
        dto.setReturnedAt(booking.getReturnedAt());
        dto.setReturnNote(booking.getReturnNote());
        dto.setCancellationReason(booking.getCancellationReason());
        dto.setCancelledBy(booking.getCancelledBy());
        dto.setCancelledAt(booking.getCancelledAt());
        dto.setAppliedCouponCode(booking.getAppliedCouponCode());
        dto.setDiscountAmount(booking.getDiscountAmount());
        dto.setTotalAmount(booking.getTotalAmount());
        if (booking.getRecurringBooking() != null) {
            dto.setRecurringBookingId(booking.getRecurringBooking().getId());
        }
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        dto.setCreatedBy(booking.getCreatedBy());
        dto.setUpdatedBy(booking.getUpdatedBy());
        return dto;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

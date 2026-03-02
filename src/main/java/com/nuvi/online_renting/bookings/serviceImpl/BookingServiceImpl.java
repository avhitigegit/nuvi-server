package com.nuvi.online_renting.bookings.serviceImpl;

import com.nuvi.online_renting.bookings.dto.BookingRequestDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final AuthenticationFacade authFacade;

    @Override
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + dto.getUserId()));
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + dto.getItemId()));

        if (!item.isAvailable()) {
            throw new BadRequestException("Item is not available for booking");
        }

        if (bookingRepository.existsOverlappingBooking(item.getId(), dto.getStartDate(), dto.getEndDate())) {
            throw new ConflictException("Item is already booked for the selected date range");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setItem(item);
        booking.setStartDate(dto.getStartDate());
        booking.setEndDate(dto.getEndDate());
        booking.setStatus(BookingStatus.PENDING.name());

        return convertToDTO(bookingRepository.save(booking));
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

        if (!BookingStatus.PENDING.name().equals(booking.getStatus())) {
            throw new BadRequestException("Only PENDING bookings can be updated");
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + dto.getUserId()));
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + dto.getItemId()));

        booking.setUser(user);
        booking.setItem(item);
        booking.setStartDate(dto.getStartDate());
        booking.setEndDate(dto.getEndDate());

        return convertToDTO(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Booking not found with id " + id);
        }
        bookingRepository.deleteById(id);
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
        return convertToDTO(bookingRepository.save(booking));
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

        return convertToDTO(bookingRepository.save(booking));
    }

    @Override
    public PagedResponse<BookingResponseDTO> getActiveBookingsByItem(Long itemId, Pageable pageable) {
        Page<Booking> page = bookingRepository.findActiveByItemId(itemId, pageable);
        return new PagedResponse<>(page.map(this::convertToDTO));
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
        dto.setReturnedAt(booking.getReturnedAt());
        dto.setReturnNote(booking.getReturnNote());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        dto.setCreatedBy(booking.getCreatedBy());
        dto.setUpdatedBy(booking.getUpdatedBy());
        return dto;
    }
}

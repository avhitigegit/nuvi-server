package com.nuvi.online_renting.bookings.serviceImpl;

import com.nuvi.online_renting.bookings.dto.BookingRequestDTO;
import com.nuvi.online_renting.bookings.dto.BookingResponseDTO;
import com.nuvi.online_renting.bookings.model.Booking;
import com.nuvi.online_renting.bookings.repository.BookingRepository;
import com.nuvi.online_renting.bookings.service.BookingService;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.BookingStatus;
import com.nuvi.online_renting.item.model.Item;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        boolean conflict = bookingRepository.existsOverlappingBooking(
                item.getId(), dto.getStartDate(), dto.getEndDate());
        if (conflict) {
            throw new RuntimeException("Item already booked for this date range");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setItem(item);
        booking.setStartDate(dto.getStartDate());
        booking.setEndDate(dto.getEndDate());
        booking.setStatus(BookingStatus.PENDING.name());

        return convertToBookingResponseDTO(bookingRepository.save(booking));
    }

    @Override
    public BookingResponseDTO getBookingById(Long id) {
        return bookingRepository.findById(id)
                .map(this::convertToBookingResponseDTO)
                .orElseThrow(() -> new RuntimeException("Booking not found with id " + id));
    }

    @Override
    public PagedResponse<BookingResponseDTO> getAllBookings(String status, Long userId, Pageable pageable) {
        Page<Booking> page = bookingRepository.filterBookings(status, userId, pageable);
        return new PagedResponse<>(page.map(this::convertToBookingResponseDTO));
    }

    @Override
    @Transactional
    public BookingResponseDTO updateBooking(Long id, BookingRequestDTO dto) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id " + id));

        if (!BookingStatus.PENDING.name().equals(booking.getStatus())) {
            throw new RuntimeException("Only PENDING bookings can be updated");
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        booking.setUser(user);
        booking.setItem(item);
        booking.setStartDate(dto.getStartDate());
        booking.setEndDate(dto.getEndDate());

        return convertToBookingResponseDTO(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    @Override
    @Transactional
    public BookingResponseDTO updateStatus(Long bookingId, BookingStatus bookingStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!BookingStatus.PENDING.name().equals(booking.getStatus())) {
            throw new RuntimeException("Only PENDING bookings can be updated");
        }

        if (bookingStatus == BookingStatus.CONFIRMED || bookingStatus == BookingStatus.CANCELLED) {
            booking.setStatus(bookingStatus.name());
        } else {
            throw new RuntimeException("Invalid status transition");
        }

        return convertToBookingResponseDTO(bookingRepository.save(booking));
    }

    private BookingResponseDTO convertToBookingResponseDTO(Booking booking) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setItemId(booking.getItem().getId());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setStatus(booking.getStatus());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        dto.setCreatedBy(booking.getCreatedBy());
        dto.setUpdatedBy(booking.getUpdatedBy());
        return dto;
    }
}

package com.nuvi.online_renting.bookings.serviceImpl;

import com.nuvi.online_renting.bookings.dto.BookingRequestDTO;
import com.nuvi.online_renting.bookings.dto.BookingResponseDTO;
import com.nuvi.online_renting.bookings.model.Booking;
import com.nuvi.online_renting.bookings.repository.BookingRepository;
import com.nuvi.online_renting.bookings.service.BookingService;
import com.nuvi.online_renting.common.dto.BookingStatus;
import com.nuvi.online_renting.item.model.Item;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

//    public BookingServiceImpl(BookingRepository bookingRepository,
//                              UserRepository userRepository,
//                              ItemRepository itemRepository) {
//        this.bookingRepository = bookingRepository;
//        this.userRepository = userRepository;
//        this.itemRepository = itemRepository;
//    }

    @Override
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO bookingRequestDTO) {
        User user = userRepository.findById(bookingRequestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Item item = itemRepository.findById(bookingRequestDTO.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Prevent double booking
        boolean conflict = bookingRepository.existsOverlappingBooking(
                item.getId(), bookingRequestDTO.getStartDate(), bookingRequestDTO.getEndDate()
        );
        if (conflict) {
            throw new RuntimeException("Item already booked for this date range");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setItem(item);
        booking.setStartDate(bookingRequestDTO.getStartDate());
        booking.setEndDate(bookingRequestDTO.getEndDate());
        booking.setStatus(BookingStatus.PENDING.toString());

        Booking savedBooking = bookingRepository.save(booking);
        return convertToBookingResponseDTO(savedBooking);
    }

    @Override
    public BookingResponseDTO getBookingById(Long id) {
        return bookingRepository.findById(id)
                .map(this::convertToBookingResponseDTO)
                .orElseThrow(() -> new RuntimeException("Booking not found with id " + id));
    }

    @Override
    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::convertToBookingResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponseDTO updateBooking(Long id, BookingRequestDTO bookingRequestDTO) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id " + id));

        if (booking.getStatus() != BookingStatus.PENDING.toString()) {
            throw new RuntimeException("Only PENDING bookings can be updated. Please Delete the booking and CREATE NEW");
        }

        User user = userRepository.findById(bookingRequestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Item item = itemRepository.findById(bookingRequestDTO.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        booking.setUser(user);
        booking.setItem(item);
        booking.setStartDate(bookingRequestDTO.getStartDate());
        booking.setEndDate(bookingRequestDTO.getEndDate());
//        booking.setStatus(bookingRequestDTO.getStatus());

        Booking updatedBooking = bookingRepository.save(booking);
        return convertToBookingResponseDTO(updatedBooking);
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    private BookingResponseDTO convertToBookingResponseDTO(Booking booking) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setItemId(booking.getItem().getId());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setStatus(booking.getStatus());
        return dto;
    }

    @Transactional
    public BookingResponseDTO updateStatus(Long bookingId, BookingStatus bookingStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus().toString() != BookingStatus.PENDING.toString()) {
            throw new RuntimeException("Only PENDING bookings can be updated");
        }

        if (bookingStatus == BookingStatus.CONFIRMED || bookingStatus == BookingStatus.CANCELLED) {
            booking.setStatus(bookingStatus.toString());
        } else {
            throw new RuntimeException("Invalid status transition");
        }

        return convertToBookingResponseDTO(bookingRepository.save(booking));
    }
}
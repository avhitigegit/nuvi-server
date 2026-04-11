package com.nuvi.online_renting.dashboard.serviceImpl;

import com.nuvi.online_renting.bookings.repository.BookingRepository;
import com.nuvi.online_renting.common.enums.BookingStatus;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.dashboard.dto.AdminAnalyticsDTO;
import com.nuvi.online_renting.dashboard.dto.AdminAnalyticsDTO.DailyStatDTO;
import com.nuvi.online_renting.dashboard.service.AdminAnalyticsService;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public AdminAnalyticsServiceImpl(BookingRepository bookingRepository,
                                     ItemRepository itemRepository,
                                     UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsDTO getAnalytics(int days) {
        if (days < 1 || days > 365) days = 30;

        LocalDateTime from = LocalDate.now().minusDays(days - 1L).atStartOfDay();

        AdminAnalyticsDTO dto = new AdminAnalyticsDTO();

        // Platform totals
        dto.setTotalUsers(userRepository.countByRole(Role.USER));
        dto.setTotalSellers(userRepository.countByRole(Role.SELLER));
        dto.setTotalItems(itemRepository.count());
        dto.setTotalBookings(bookingRepository.count());
        dto.setTotalRevenue(round2(bookingRepository.sumTotalRevenue()));

        // Booking status breakdown
        dto.setPendingBookings(bookingRepository.countByStatus(BookingStatus.PENDING.name()));
        dto.setConfirmedBookings(bookingRepository.countByStatus(BookingStatus.CONFIRMED.name()));
        dto.setCompletedBookings(bookingRepository.countByStatus(BookingStatus.COMPLETED.name()));
        dto.setCancelledBookings(bookingRepository.countByStatus(BookingStatus.CANCELLED.name()));

        // Time-series
        dto.setDailyBookings(toDailyStats(bookingRepository.findDailyBookingCounts(from)));
        dto.setDailyRevenue(toDailyStats(bookingRepository.findDailyRevenue(from)));

        return dto;
    }

    private List<DailyStatDTO> toDailyStats(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new DailyStatDTO(
                        (String) row[0],
                        round2(((Number) row[1]).doubleValue())
                ))
                .toList();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

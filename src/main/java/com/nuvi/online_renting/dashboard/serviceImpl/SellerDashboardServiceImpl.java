package com.nuvi.online_renting.dashboard.serviceImpl;

import com.nuvi.online_renting.bookings.repository.BookingRepository;
import com.nuvi.online_renting.common.enums.BookingStatus;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.dashboard.dto.SellerDashboardDTO;
import com.nuvi.online_renting.dashboard.service.SellerDashboardService;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class SellerDashboardServiceImpl implements SellerDashboardService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final AuthenticationFacade authFacade;

    public SellerDashboardServiceImpl(BookingRepository bookingRepository,
                                      ItemRepository itemRepository,
                                      UserRepository userRepository,
                                      AuthenticationFacade authFacade) {
        this.bookingRepository = bookingRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.authFacade = authFacade;
    }

    @Override
    @Transactional(readOnly = true)
    public SellerDashboardDTO getDashboard(Long sellerId) {
        User currentUser = authFacade.getCurrentUser();

        // Sellers see their own dashboard; admins can query any seller
        if (currentUser.getRole() != Role.ADMIN) {
            sellerId = currentUser.getId();
        } else if (sellerId == null) {
            throw new ResourceNotFoundException("sellerId query parameter is required for admin dashboard queries");
        }

        final Long resolvedSellerId = sellerId;

        // Validate the target seller exists
        User seller = userRepository.findById(resolvedSellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id " + resolvedSellerId));

        if (seller.getRole() != Role.SELLER && seller.getRole() != Role.ADMIN) {
            throw new ForbiddenException("User " + resolvedSellerId + " is not a seller");
        }

        LocalDateTime startOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime startOfWeek  = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();

        SellerDashboardDTO dto = new SellerDashboardDTO();

        // Items
        dto.setTotalItems(itemRepository.countBySellerId(resolvedSellerId));
        dto.setActiveItems(itemRepository.countBySellerIdAndAvailable(resolvedSellerId, true));
        dto.setInactiveItems(itemRepository.countBySellerIdAndAvailable(resolvedSellerId, false));

        // Seller rating (from User entity — kept in sync by review submission)
        dto.setAverageRating(seller.getAverageRating());
        dto.setTotalReviews(seller.getTotalReviews());

        // Bookings
        dto.setTotalBookings(bookingRepository.countBySellerId(resolvedSellerId));
        dto.setPendingBookings(bookingRepository.countBySellerIdAndStatus(resolvedSellerId, BookingStatus.PENDING.name()));
        dto.setConfirmedBookings(bookingRepository.countBySellerIdAndStatus(resolvedSellerId, BookingStatus.CONFIRMED.name()));
        dto.setCompletedBookings(bookingRepository.countBySellerIdAndStatus(resolvedSellerId, BookingStatus.COMPLETED.name()));
        dto.setCancelledBookings(bookingRepository.countBySellerIdAndStatus(resolvedSellerId, BookingStatus.CANCELLED.name()));
        dto.setBookingsThisMonth(bookingRepository.countBySellerIdSince(resolvedSellerId, startOfMonth));
        dto.setBookingsThisWeek(bookingRepository.countBySellerIdSince(resolvedSellerId, startOfWeek));

        // Earnings (COMPLETED bookings only)
        dto.setTotalEarnings(round2(bookingRepository.sumEarningsBySellerId(resolvedSellerId)));
        dto.setEarningsThisMonth(round2(bookingRepository.sumEarningsBySellerIdSince(resolvedSellerId, startOfMonth)));
        dto.setEarningsThisWeek(round2(bookingRepository.sumEarningsBySellerIdSince(resolvedSellerId, startOfWeek)));

        // Top 5 items
        List<Object[]> topRaw = bookingRepository.findTopItemsBySellerId(resolvedSellerId, PageRequest.of(0, 5));
        List<SellerDashboardDTO.TopItemDTO> topItems = topRaw.stream().map(row -> {
            SellerDashboardDTO.TopItemDTO t = new SellerDashboardDTO.TopItemDTO();
            t.setItemId(((Number) row[0]).longValue());
            t.setItemName((String) row[1]);
            t.setCompletedBookings(((Number) row[2]).longValue());
            t.setTotalEarnings(round2(((Number) row[3]).doubleValue()));
            return t;
        }).toList();
        dto.setTopItems(topItems);

        return dto;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

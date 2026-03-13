package com.nuvi.online_renting.reviews.serviceImpl;

import com.nuvi.online_renting.bookings.model.Booking;
import com.nuvi.online_renting.bookings.repository.BookingRepository;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.BookingStatus;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ConflictException;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.reviews.dto.SellerReviewRequestDTO;
import com.nuvi.online_renting.reviews.dto.SellerReviewResponseDTO;
import com.nuvi.online_renting.reviews.model.SellerReview;
import com.nuvi.online_renting.reviews.repository.SellerReviewRepository;
import com.nuvi.online_renting.reviews.service.SellerReviewService;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SellerReviewServiceImpl implements SellerReviewService {

    private static final Logger log = LoggerFactory.getLogger(SellerReviewServiceImpl.class);

    private final SellerReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AuthenticationFacade authFacade;

    @Override
    @Transactional
    public SellerReviewResponseDTO submitReview(SellerReviewRequestDTO dto) {
        User reviewer = authFacade.getCurrentUser();

        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + dto.getBookingId()));

        // Only the renter of the booking can leave a review
        if (!booking.getUser().getId().equals(reviewer.getId())) {
            throw new ForbiddenException("You can only review a seller for your own bookings");
        }

        // Only COMPLETED bookings can be reviewed
        if (!BookingStatus.COMPLETED.name().equals(booking.getStatus())) {
            throw new BadRequestException("You can only review a seller after the booking is COMPLETED");
        }

        // One review per booking
        if (reviewRepository.existsByBookingIdAndReviewerId(booking.getId(), reviewer.getId())) {
            throw new ConflictException("You have already submitted a review for this booking");
        }

        User seller = booking.getItem().getSeller();

        SellerReview review = new SellerReview();
        review.setBooking(booking);
        review.setSeller(seller);
        review.setReviewer(reviewer);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        reviewRepository.save(review);

        // Recalculate and update seller's average rating
        double newAverage = reviewRepository.computeAverageRating(seller.getId());
        long newTotal = reviewRepository.countBySellerId(seller.getId());
        seller.setAverageRating(Math.round(newAverage * 10.0) / 10.0);
        seller.setTotalReviews((int) newTotal);
        userRepository.save(seller);

        log.info("Review submitted by user {} for seller {} (booking {})", reviewer.getId(), seller.getId(), booking.getId());
        return toDTO(review);
    }

    @Override
    public SellerReviewResponseDTO getReviewById(Long id) {
        return toDTO(reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id " + id)));
    }

    @Override
    public PagedResponse<SellerReviewResponseDTO> getReviewsBySeller(Long sellerId, Pageable pageable) {
        if (!userRepository.existsById(sellerId)) {
            throw new ResourceNotFoundException("Seller not found with id " + sellerId);
        }
        return new PagedResponse<>(reviewRepository.findBySellerId(sellerId, pageable).map(this::toDTO));
    }

    private SellerReviewResponseDTO toDTO(SellerReview review) {
        SellerReviewResponseDTO dto = new SellerReviewResponseDTO();
        dto.setId(review.getId());
        dto.setBookingId(review.getBooking().getId());
        dto.setSellerId(review.getSeller().getId());
        dto.setSellerName(review.getSeller().getName());
        dto.setReviewerId(review.getReviewer().getId());
        dto.setReviewerName(review.getReviewer().getName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}

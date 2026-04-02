package com.nuvi.online_renting.itemreviews.serviceImpl;

import com.nuvi.online_renting.bookings.model.Booking;
import com.nuvi.online_renting.bookings.repository.BookingRepository;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.BookingStatus;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ConflictException;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.item.model.Item;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.itemreviews.dto.ItemReviewRequestDTO;
import com.nuvi.online_renting.itemreviews.dto.ItemReviewResponseDTO;
import com.nuvi.online_renting.itemreviews.model.ItemReview;
import com.nuvi.online_renting.itemreviews.repository.ItemReviewRepository;
import com.nuvi.online_renting.itemreviews.service.ItemReviewService;
import com.nuvi.online_renting.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemReviewServiceImpl implements ItemReviewService {

    private final ItemReviewRepository itemReviewRepository;
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final AuthenticationFacade authFacade;

    public ItemReviewServiceImpl(ItemReviewRepository itemReviewRepository,
                                 BookingRepository bookingRepository,
                                 ItemRepository itemRepository,
                                 AuthenticationFacade authFacade) {
        this.itemReviewRepository = itemReviewRepository;
        this.bookingRepository = bookingRepository;
        this.itemRepository = itemRepository;
        this.authFacade = authFacade;
    }

    @Override
    @Transactional
    public ItemReviewResponseDTO submitReview(ItemReviewRequestDTO dto) {
        User reviewer = authFacade.getCurrentUser();

        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + dto.getBookingId()));

        if (!booking.getUser().getId().equals(reviewer.getId())) {
            throw new ForbiddenException("You can only review items from your own bookings");
        }

        if (!BookingStatus.COMPLETED.name().equals(booking.getStatus())) {
            throw new BadRequestException("You can only review an item after the booking is COMPLETED");
        }

        if (itemReviewRepository.existsByBookingIdAndReviewerId(booking.getId(), reviewer.getId())) {
            throw new ConflictException("You have already submitted a review for this booking");
        }

        Item item = booking.getItem();

        ItemReview review = new ItemReview();
        review.setBooking(booking);
        review.setItem(item);
        review.setReviewer(reviewer);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        itemReviewRepository.save(review);

        // Update item's aggregate rating
        double newAvg = itemReviewRepository.calculateAverageRating(item.getId());
        long totalReviews = itemReviewRepository.countByItemId(item.getId());
        item.setAverageRating(Math.round(newAvg * 10.0) / 10.0);
        item.setTotalReviews((int) totalReviews);
        itemRepository.save(item);

        return toDTO(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemReviewResponseDTO getReviewById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ItemReviewResponseDTO> getReviewsByItem(Long itemId, Pageable pageable) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found with id " + itemId);
        }
        Page<ItemReview> page = itemReviewRepository.findByItemId(itemId, pageable);
        return new PagedResponse<>(page.map(this::toDTO));
    }

    private ItemReview findOrThrow(Long id) {
        return itemReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item review not found with id " + id));
    }

    private ItemReviewResponseDTO toDTO(ItemReview review) {
        ItemReviewResponseDTO dto = new ItemReviewResponseDTO();
        dto.setId(review.getId());
        dto.setBookingId(review.getBooking().getId());
        dto.setItemId(review.getItem().getId());
        dto.setItemName(review.getItem().getName());
        dto.setReviewerId(review.getReviewer().getId());
        dto.setReviewerName(review.getReviewer().getName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }
}

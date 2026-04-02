package com.nuvi.online_renting.recurring.serviceImpl;

import com.nuvi.online_renting.bookings.model.Booking;
import com.nuvi.online_renting.bookings.repository.BookingRepository;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.BookingStatus;
import com.nuvi.online_renting.common.enums.RecurrenceFrequency;
import com.nuvi.online_renting.common.enums.RecurringBookingStatus;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ConflictException;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.item.model.Item;
import com.nuvi.online_renting.item.repository.ItemBlockedDateRepository;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.recurring.dto.RecurringBookingRequestDTO;
import com.nuvi.online_renting.recurring.dto.RecurringBookingResponseDTO;
import com.nuvi.online_renting.recurring.model.RecurringBooking;
import com.nuvi.online_renting.recurring.repository.RecurringBookingRepository;
import com.nuvi.online_renting.recurring.service.RecurringBookingService;
import com.nuvi.online_renting.users.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class RecurringBookingServiceImpl implements RecurringBookingService {

    private static final Logger log = LoggerFactory.getLogger(RecurringBookingServiceImpl.class);

    private final RecurringBookingRepository recurringBookingRepository;
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final ItemBlockedDateRepository blockedDateRepository;
    private final AuthenticationFacade authFacade;

    public RecurringBookingServiceImpl(RecurringBookingRepository recurringBookingRepository,
                                       BookingRepository bookingRepository,
                                       ItemRepository itemRepository,
                                       ItemBlockedDateRepository blockedDateRepository,
                                       AuthenticationFacade authFacade) {
        this.recurringBookingRepository = recurringBookingRepository;
        this.bookingRepository = bookingRepository;
        this.itemRepository = itemRepository;
        this.blockedDateRepository = blockedDateRepository;
        this.authFacade = authFacade;
    }

    @Override
    @Transactional
    public RecurringBookingResponseDTO createTemplate(RecurringBookingRequestDTO request) {
        User currentUser = authFacade.getCurrentUser();

        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + request.getItemId()));

        if (!item.isAvailable()) {
            throw new BadRequestException("Item is not available for booking");
        }
        if (item.getSeller().isSuspended()) {
            throw new BadRequestException("This item cannot be booked because the seller's account has been suspended");
        }

        LocalDate firstStart = request.getFirstOccurrenceDate();
        LocalDate firstEnd = firstStart.plusDays(request.getDurationDays());

        // Validate and create the first occurrence immediately
        checkAvailability(item.getId(), firstStart, firstEnd);

        Booking firstBooking = new Booking();
        firstBooking.setUser(currentUser);
        firstBooking.setItem(item);
        firstBooking.setStartDate(firstStart);
        firstBooking.setEndDate(firstEnd);
        firstBooking.setStatus(BookingStatus.PENDING.name());

        RecurringBooking template = new RecurringBooking();
        template.setUser(currentUser);
        template.setItem(item);
        template.setDurationDays(request.getDurationDays());
        template.setFrequency(request.getFrequency());
        template.setFirstOccurrenceDate(firstStart);
        template.setNextOccurrenceDate(advanceDate(firstStart, request.getFrequency()));
        template.setEndAfterDate(request.getEndAfterDate());
        template.setMaxOccurrences(request.getMaxOccurrences());
        template.setOccurrencesCreated(1);
        template.setStatus(RecurringBookingStatus.ACTIVE);

        RecurringBooking saved = recurringBookingRepository.save(template);

        firstBooking.setRecurringBooking(saved);
        bookingRepository.save(firstBooking);

        log.info("Recurring booking template {} created by user {} for item {}", saved.getId(), currentUser.getId(), item.getId());
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RecurringBookingResponseDTO> getMyTemplates(Pageable pageable) {
        User currentUser = authFacade.getCurrentUser();
        return new PagedResponse<>(recurringBookingRepository.findByUserId(currentUser.getId(), pageable).map(this::toDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringBookingResponseDTO getTemplate(Long id) {
        RecurringBooking template = getOrThrow(id);
        validateOwner(template);
        return toDTO(template);
    }

    @Override
    @Transactional
    public RecurringBookingResponseDTO pauseTemplate(Long id) {
        RecurringBooking template = getOrThrow(id);
        validateOwner(template);
        if (template.getStatus() != RecurringBookingStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE templates can be paused");
        }
        template.setStatus(RecurringBookingStatus.PAUSED);
        return toDTO(recurringBookingRepository.save(template));
    }

    @Override
    @Transactional
    public RecurringBookingResponseDTO resumeTemplate(Long id) {
        RecurringBooking template = getOrThrow(id);
        validateOwner(template);
        if (template.getStatus() != RecurringBookingStatus.PAUSED) {
            throw new BadRequestException("Only PAUSED templates can be resumed");
        }
        template.setStatus(RecurringBookingStatus.ACTIVE);
        return toDTO(recurringBookingRepository.save(template));
    }

    @Override
    @Transactional
    public RecurringBookingResponseDTO cancelTemplate(Long id) {
        RecurringBooking template = getOrThrow(id);
        validateOwner(template);
        if (template.getStatus() == RecurringBookingStatus.CANCELLED) {
            throw new BadRequestException("Template is already cancelled");
        }
        template.setStatus(RecurringBookingStatus.CANCELLED);
        return toDTO(recurringBookingRepository.save(template));
    }

    // -------------------------------------------------------------------------
    // Scheduler entry point — called by RecurringBookingScheduler
    // -------------------------------------------------------------------------

    @Transactional
    public void processDueTemplates() {
        LocalDate horizon = LocalDate.now().plusDays(3);
        recurringBookingRepository.findDueTemplates(horizon).forEach(this::createNextOccurrence);
    }

    private void createNextOccurrence(RecurringBooking template) {
        // Stop conditions
        if (template.getMaxOccurrences() != null &&
                template.getOccurrencesCreated() >= template.getMaxOccurrences()) {
            template.setStatus(RecurringBookingStatus.CANCELLED);
            recurringBookingRepository.save(template);
            return;
        }

        LocalDate nextStart = template.getNextOccurrenceDate();

        if (template.getEndAfterDate() != null && nextStart.isAfter(template.getEndAfterDate())) {
            template.setStatus(RecurringBookingStatus.CANCELLED);
            recurringBookingRepository.save(template);
            return;
        }

        LocalDate nextEnd = nextStart.plusDays(template.getDurationDays());
        Long itemId = template.getItem().getId();

        if (bookingRepository.existsOverlappingBooking(itemId, nextStart, nextEnd) ||
                blockedDateRepository.existsOverlappingBlock(itemId, nextStart, nextEnd)) {
            log.warn("Recurring booking {} — skipping occurrence on {} due to overlap", template.getId(), nextStart);
            // Advance the pointer anyway to avoid re-attempting the blocked slot indefinitely
            template.setNextOccurrenceDate(advanceDate(nextStart, template.getFrequency()));
            recurringBookingRepository.save(template);
            return;
        }

        Booking booking = new Booking();
        booking.setUser(template.getUser());
        booking.setItem(template.getItem());
        booking.setStartDate(nextStart);
        booking.setEndDate(nextEnd);
        booking.setStatus(BookingStatus.PENDING.name());
        booking.setRecurringBooking(template);
        bookingRepository.save(booking);

        template.setOccurrencesCreated(template.getOccurrencesCreated() + 1);
        template.setNextOccurrenceDate(advanceDate(nextStart, template.getFrequency()));
        recurringBookingRepository.save(template);

        log.info("Recurring booking {} — created occurrence {} starting {}", template.getId(),
                template.getOccurrencesCreated(), nextStart);
    }

    // -------------------------------------------------------------------------

    private void checkAvailability(Long itemId, LocalDate start, LocalDate end) {
        if (bookingRepository.existsOverlappingBooking(itemId, start, end)) {
            throw new ConflictException("Item is already booked for the selected date range");
        }
        if (blockedDateRepository.existsOverlappingBlock(itemId, start, end)) {
            throw new ConflictException("The item is not available for the selected dates — the seller has blocked this period");
        }
    }

    private LocalDate advanceDate(LocalDate from, RecurrenceFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> from.plusWeeks(1);
            case BIWEEKLY -> from.plusWeeks(2);
            case MONTHLY -> from.plusMonths(1);
        };
    }

    private void validateOwner(RecurringBooking template) {
        User currentUser = authFacade.getCurrentUser();
        if (!template.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have access to this recurring booking");
        }
    }

    private RecurringBooking getOrThrow(Long id) {
        return recurringBookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring booking template not found with id " + id));
    }

    private RecurringBookingResponseDTO toDTO(RecurringBooking t) {
        RecurringBookingResponseDTO dto = new RecurringBookingResponseDTO();
        dto.setId(t.getId());
        dto.setUserId(t.getUser().getId());
        dto.setUserName(t.getUser().getName());
        dto.setItemId(t.getItem().getId());
        dto.setItemName(t.getItem().getName());
        dto.setDurationDays(t.getDurationDays());
        dto.setFrequency(t.getFrequency());
        dto.setFirstOccurrenceDate(t.getFirstOccurrenceDate());
        dto.setNextOccurrenceDate(t.getNextOccurrenceDate());
        dto.setEndAfterDate(t.getEndAfterDate());
        dto.setMaxOccurrences(t.getMaxOccurrences());
        dto.setOccurrencesCreated(t.getOccurrencesCreated());
        dto.setStatus(t.getStatus());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }
}

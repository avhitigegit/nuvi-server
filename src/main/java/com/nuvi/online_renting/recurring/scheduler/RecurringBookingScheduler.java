package com.nuvi.online_renting.recurring.scheduler;

import com.nuvi.online_renting.recurring.serviceImpl.RecurringBookingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs once every day at 06:00 AM (server time) and creates upcoming booking
 * occurrences for all ACTIVE recurring booking templates whose next occurrence
 * date falls within a 3-day scheduling horizon.
 *
 * The 3-day lookahead gives the seller time to confirm or reject the booking
 * before the rental period begins.
 */
@Component
public class RecurringBookingScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringBookingScheduler.class);

    private final RecurringBookingServiceImpl recurringBookingService;

    public RecurringBookingScheduler(RecurringBookingServiceImpl recurringBookingService) {
        this.recurringBookingService = recurringBookingService;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void processDueRecurringBookings() {
        log.info("RecurringBookingScheduler — processing due templates");
        recurringBookingService.processDueTemplates();
        log.info("RecurringBookingScheduler — done");
    }
}

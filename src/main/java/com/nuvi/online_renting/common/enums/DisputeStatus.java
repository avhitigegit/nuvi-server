package com.nuvi.online_renting.common.enums;

public enum DisputeStatus {
    OPEN,           // Raised by renter, waiting for admin review
    UNDER_REVIEW,   // Admin has picked it up
    RESOLVED,       // Admin resolved in renter's favour
    REJECTED        // Admin rejected the dispute
}

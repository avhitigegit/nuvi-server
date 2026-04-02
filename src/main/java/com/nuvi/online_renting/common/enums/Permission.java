package com.nuvi.online_renting.common.enums;

public enum Permission {

    // Auth & profile
    VIEW_OWN_PROFILE,
    UPDATE_OWN_PROFILE,
    DEACTIVATE_OWN_ACCOUNT,


    // Items
    VIEW_ITEMS,
    CREATE_ITEM,
    UPDATE_OWN_ITEM,
    DELETE_OWN_ITEM,

    // Bookings
    CREATE_BOOKING,
    VIEW_OWN_BOOKINGS,
    CANCEL_OWN_BOOKING,
    CONFIRM_OWN_BOOKING,
    REJECT_OWN_BOOKING,

    // Seller
    APPLY_SELLER,
    UPLOAD_SELLER_DOCS,

    // Disputes
    RAISE_DISPUTE,
    VIEW_OWN_DISPUTES,

    // Reviews
    WRITE_REVIEW,
    VIEW_REVIEWS,

    // Admin
    VIEW_ALL_USERS,
    VIEW_ALL_BOOKINGS,
    MANAGE_SELLER_APPLICATIONS,
    UPDATE_BOOKING_STATUS,
    MANAGE_DISPUTES,
    SUSPEND_SELLER,
    MANAGE_CATEGORIES,
    MANAGE_KYC,
    FULL_ACCESS

    }

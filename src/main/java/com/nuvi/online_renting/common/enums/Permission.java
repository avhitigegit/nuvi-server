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

    // Seller
    APPLY_SELLER,
    UPLOAD_SELLER_DOCS,

    // Admin
    VIEW_ALL_USERS,
    VIEW_ALL_BOOKINGS,
    MANAGE_SELLER_APPLICATIONS,
    UPDATE_BOOKING_STATUS,
    FULL_ACCESS

    }

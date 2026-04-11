package com.nuvi.online_renting.common.security;

import com.nuvi.online_renting.common.enums.Permission;
import com.nuvi.online_renting.common.enums.Role;

import static com.nuvi.online_renting.common.enums.Permission.*;

import java.util.Set;

public class RolePermissionMapper {

    public static Set<Permission> getPermissions(Role role) {

        return switch (role) {

            case USER -> Set.of(
                    VIEW_OWN_PROFILE,
                    UPDATE_OWN_PROFILE,
                    DEACTIVATE_OWN_ACCOUNT,
                    VIEW_ITEMS,
                    CREATE_BOOKING,
                    VIEW_OWN_BOOKINGS,
                    CANCEL_OWN_BOOKING,
                    APPLY_SELLER,
                    UPLOAD_SELLER_DOCS,
                    RAISE_DISPUTE,
                    VIEW_OWN_DISPUTES,
                    WRITE_REVIEW,
                    VIEW_REVIEWS,
                    SEND_MESSAGE
            );

            case SELLER -> Set.of(
                    VIEW_OWN_PROFILE,
                    UPDATE_OWN_PROFILE,
                    VIEW_ITEMS,
                    CREATE_ITEM,
                    UPDATE_OWN_ITEM,
                    DELETE_OWN_ITEM,
                    CREATE_BOOKING,
                    VIEW_OWN_BOOKINGS,
                    DEACTIVATE_OWN_ACCOUNT,
                    CANCEL_OWN_BOOKING,
                    CONFIRM_OWN_BOOKING,
                    REJECT_OWN_BOOKING,
                    COMPLETE_OWN_BOOKING,
                    RAISE_DISPUTE,
                    VIEW_OWN_DISPUTES,
                    WRITE_REVIEW,
                    VIEW_REVIEWS,
                    SEND_MESSAGE,
                    SUBMIT_APPEAL
            );

            case ADMIN -> Set.of(
                    VIEW_OWN_PROFILE,
                    UPDATE_OWN_PROFILE,
                    DEACTIVATE_OWN_ACCOUNT,
                    VIEW_ITEMS,
                    CREATE_ITEM,
                    UPDATE_OWN_ITEM,
                    DELETE_OWN_ITEM,
                    CREATE_BOOKING,
                    VIEW_OWN_BOOKINGS,
                    CANCEL_OWN_BOOKING,
                    CONFIRM_OWN_BOOKING,
                    REJECT_OWN_BOOKING,
                    APPLY_SELLER,
                    UPLOAD_SELLER_DOCS,
                    VIEW_ALL_USERS,
                    VIEW_ALL_BOOKINGS,
                    MANAGE_SELLER_APPLICATIONS,
                    UPDATE_BOOKING_STATUS,
                    MANAGE_DISPUTES,
                    SUSPEND_SELLER,
                    MANAGE_CATEGORIES,
                    MANAGE_KYC,
                    VIEW_REVIEWS,
                    FULL_ACCESS
            );
        };
    }
}

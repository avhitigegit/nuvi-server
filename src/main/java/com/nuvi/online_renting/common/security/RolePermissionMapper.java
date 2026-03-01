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
                    UPLOAD_SELLER_DOCS
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
                    CANCEL_OWN_BOOKING
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
                    APPLY_SELLER,
                    UPLOAD_SELLER_DOCS,
                    VIEW_ALL_USERS,
                    VIEW_ALL_BOOKINGS,
                    MANAGE_SELLER_APPLICATIONS,
                    UPDATE_BOOKING_STATUS,
                    FULL_ACCESS
            );
        };
    }
}

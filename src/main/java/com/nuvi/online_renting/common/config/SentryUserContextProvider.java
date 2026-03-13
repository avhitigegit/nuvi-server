package com.nuvi.online_renting.common.config;

import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.users.model.User;
import io.sentry.protocol.SentryId;
import io.sentry.spring.jakarta.SentryUserProvider;
import org.springframework.stereotype.Component;

/**
 * Attaches the currently authenticated user's details to every Sentry error event.
 *
 * When an error is captured, Sentry will display:
 *   User ID : 42
 *   Email   : john@example.com
 *   Username: John Doe
 *
 * This makes it easy to identify which user encountered the error.
 * For unauthenticated requests the user context is left empty.
 */
@Component
public class SentryUserContextProvider implements SentryUserProvider {

    private final AuthenticationFacade authFacade;

    public SentryUserContextProvider(AuthenticationFacade authFacade) {
        this.authFacade = authFacade;
    }

    @Override
    public io.sentry.protocol.User provideUser() {
        try {
            User user = authFacade.getCurrentUser();
            if (user == null) {
                return null; // unauthenticated request — no user context to attach
            }
            io.sentry.protocol.User sentryUser = new io.sentry.protocol.User();
            sentryUser.setId(String.valueOf(user.getId()));
            sentryUser.setEmail(user.getEmail());
            sentryUser.setUsername(user.getName());
            return sentryUser;
        } catch (Exception e) {
            // Never let Sentry context setup break the request
            return null;
        }
    }
}

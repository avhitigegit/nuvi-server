package com.nuvi.online_renting.common.config;

import com.nuvi.online_renting.common.security.JwtTokenService;
import com.nuvi.online_renting.common.security.RolePermissionMapper;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * WebSocket configuration with STOMP messaging.
 *
 * Connect endpoint : ws://<host>/ws  (SockJS fallback available)
 * Subscribe topics : /topic/booking/{bookingId}/messages
 * Send destination : /app/booking/{bookingId}/send
 *
 * Authentication is performed on CONNECT via the Authorization STOMP header
 * (Bearer <JWT>). The resolved principal is attached to the session so all
 * downstream @MessageMapping handlers can access the authenticated user.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    public WebSocketConfig(JwtTokenService jwtTokenService, UserRepository userRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        // In-memory simple broker for /topic destinations
        registry.enableSimpleBroker("/topic");
        // Prefix for client-to-server messages handled by @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Intercepts STOMP CONNECT frames to authenticate via JWT.
     * The token is read from the native STOMP "Authorization" header.
     */
    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            String email = jwtTokenService.extractUsername(token);
                            Optional<User> userOpt = userRepository.findByEmail(email);
                            if (userOpt.isPresent()) {
                                User user = userOpt.get();
                                List<SimpleGrantedAuthority> authorities =
                                        Stream.concat(
                                                Stream.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                                                RolePermissionMapper.getPermissions(user.getRole()).stream()
                                                        .map(p -> new SimpleGrantedAuthority(p.name()))
                                        ).collect(Collectors.toList());

                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                                accessor.setUser(auth);
                                log.debug("WebSocket CONNECT authenticated: {}", email);
                            }
                        } catch (Exception e) {
                            log.warn("WebSocket JWT validation failed: {}", e.getMessage());
                        }
                    }
                }
                return message;
            }
        });
    }
}

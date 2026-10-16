
package com.cabbooking;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String SESSION_TOKEN_KEY =
            "WEBSOCKET_JWT_TOKEN";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;

    public WebSocketAuthInterceptor(
            JwtService jwtService,
            UserRepository userRepository,
            RideRepository rideRepository,
            DriverRepository driverRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (command == StompCommand.CONNECT) {
            authenticateConnection(accessor);
            return message;
        }

        if (command == StompCommand.DISCONNECT) {
            return message;
        }

        Principal principal = accessor.getUser();

        if (principal == null) {
            throw new IllegalArgumentException(
                    "Authentication is required.");
        }

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        String token = getSessionToken(accessor);

        if (!jwtService.isTokenValid(token, user)) {
            throw new IllegalArgumentException(
                    "Invalid or expired JWT.");
        }

        switch (command) {
            case SUBSCRIBE ->
                    authorizeSubscription(
                            accessor.getDestination(), user);

            case SEND ->
                    authorizeSend(
                            accessor.getDestination(), user);

            case UNSUBSCRIBE, ACK, NACK -> {
                // Allow normal subscription lifecycle operations.
            }

            default -> throw new IllegalArgumentException(
                    "STOMP command is not allowed.");
        }

        return message;
    }

    private void authenticateConnection(
            StompHeaderAccessor accessor) {

        String authorization = accessor.getFirstNativeHeader(
                HttpHeaders.AUTHORIZATION);

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException(
                    "A Bearer JWT is required to connect.");
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            throw new IllegalArgumentException(
                    "A valid JWT is required.");
        }

        try {
            Claims claims = jwtService.extractClaims(token);
            String email = claims.getSubject();

            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException(
                        "JWT subject is missing.");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new IllegalArgumentException("User not found."));

            if (!jwtService.isTokenValid(token, user)) {
                throw new IllegalArgumentException(
                        "Invalid or expired JWT.");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + user.getRole().name()))
                    );

            accessor.setUser(authentication);

            if (accessor.getSessionAttributes() == null) {
                throw new IllegalArgumentException(
                        "WebSocket session is unavailable.");
            }

            accessor.getSessionAttributes().put(
                    SESSION_TOKEN_KEY, token);

        } catch (JwtException exception) {
            throw new IllegalArgumentException(
                    "Invalid or expired JWT.");
        }
    }

    private String getSessionToken(
            StompHeaderAccessor accessor) {

        if (accessor.getSessionAttributes() == null) {
            throw new IllegalArgumentException(
                    "WebSocket session is unavailable.");
        }

        Object value = accessor.getSessionAttributes()
                .get(SESSION_TOKEN_KEY);

        if (!(value instanceof String token) || token.isBlank()) {
            throw new IllegalArgumentException(
                    "WebSocket session is not authenticated.");
        }

        return token;
    }

    private void authorizeSubscription(
            String destination, User user) {

        if (destination == null) {
            throw new IllegalArgumentException(
                    "Subscription destination is required.");
        }

        Long rideId;

        if (destination.startsWith("/topic/rides/")
                && destination.endsWith("/location")) {
            rideId = extractRideId(
                    destination, "/topic/rides/", "/location");

        } else if (destination.startsWith("/topic/rides/")
                && destination.endsWith("/status")) {
            rideId = extractRideId(
                    destination, "/topic/rides/", "/status");

        } else {
            throw new IllegalArgumentException(
                    "Subscription destination is not allowed.");
        }

        if (rideId == null) {
            throw new IllegalArgumentException(
                    "Invalid ride subscription destination.");
        }

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ride not found."));

        if (!isRideParticipant(ride, user)) {
            throw new IllegalArgumentException(
                    "You are not authorized to track this ride.");
        }

        if (ride.getStatus() != Ride.RideStatus.ACCEPTED
                && ride.getStatus() != Ride.RideStatus.DRIVER_ARRIVING
                && ride.getStatus() != Ride.RideStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Tracking is unavailable for this ride status.");
        }
    }

    private void authorizeSend(
            String destination, User user) {

        if (destination == null
                || !destination.equals("/app/rides/location")) {
            throw new IllegalArgumentException(
                    "Message destination is not allowed.");
        }

        if (user.getRole() != User.Role.DRIVER) {
            throw new IllegalArgumentException(
                    "Only drivers can send location updates.");
        }
    }

    private boolean isRideParticipant(Ride ride, User user) {

        if (ride.getCustomer() != null
                && ride.getCustomer().getId().equals(user.getId())) {
            return true;
        }

        if (ride.getDriver() != null
                && user.getRole() == User.Role.DRIVER) {

            Driver driver = driverRepository.findByUserId(user.getId())
                    .orElse(null);

            return driver != null
                    && driver.getId().equals(ride.getDriver().getId());
        }

        return false;
    }

    private Long extractRideId(
            String destination,
            String prefix,
            String suffix) {

        if (!destination.startsWith(prefix)
                || !destination.endsWith(suffix)) {
            return null;
        }

        String rideIdText = destination.substring(
                prefix.length(),
                destination.length() - suffix.length());

        try {
            Long rideId = Long.parseLong(rideIdText);
            return rideId > 0 ? rideId : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}

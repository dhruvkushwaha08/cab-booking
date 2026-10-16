
package com.cabbooking;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@Validated
public class DriverLocationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    public DriverLocationController(
            SimpMessagingTemplate messagingTemplate,
            RideRepository rideRepository,
            UserRepository userRepository,
            DriverRepository driverRepository) {
        this.messagingTemplate = messagingTemplate;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
    }

    @MessageMapping("/rides/location")
    public void updateLocation(
            @Valid @Payload DriverLocationUpdate update,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Authentication is required.");
        }

        if (update == null
                || update.getRideId() == null
                || update.getRideId() <= 0) {
            throw new IllegalArgumentException(
                    "A valid ride ID is required.");
        }

        if (update.getLatitude() == null
                || update.getLongitude() == null
                || !Double.isFinite(update.getLatitude())
                || !Double.isFinite(update.getLongitude())
                || update.getLatitude() < -90
                || update.getLatitude() > 90
                || update.getLongitude() < -180
                || update.getLongitude() > 180) {
            throw new IllegalArgumentException(
                    "Valid latitude and longitude are required.");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (user.getRole() != User.Role.DRIVER) {
            throw new IllegalArgumentException(
                    "Only drivers can send location updates.");
        }

        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver profile not found."));

        Ride ride = rideRepository.findById(update.getRideId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Ride not found."));

        if (ride.getDriver() == null
                || !ride.getDriver().getId().equals(driver.getId())) {
            throw new IllegalArgumentException(
                    "You are not assigned to this ride.");
        }

        if (ride.getStatus() != Ride.RideStatus.ACCEPTED
                && ride.getStatus() != Ride.RideStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Location updates are allowed only for "
                            + "accepted or in-progress rides.");
        }

        Map<String, Object> location = Map.of(
                "rideId", ride.getId(),
                "latitude", update.getLatitude(),
                "longitude", update.getLongitude(),
                "timestamp", LocalDateTime.now().toString()
        );

        messagingTemplate.convertAndSend(
                "/topic/rides/" + ride.getId() + "/location",
                (Object) location
        );
    }
}

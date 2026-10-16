
package com.cabbooking;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver/availability")
public class DriverAvailabilityController {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;

    public DriverAvailabilityController(
            UserRepository userRepository,
            DriverRepository driverRepository,
            RideRepository rideRepository) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
    }

    @PutMapping
    @Transactional
    public ResponseEntity<?> updateAvailability(
            @RequestBody AvailabilityRequest request,
            Authentication authentication) {

        if (request == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Availability request is required."));
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (user.getRole() != User.Role.DRIVER) {
            return ResponseEntity.status(403).body(
                    Map.of("error",
                            "Only drivers can change availability."));
        }

        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver profile not found."));

        if (!request.available()) {
            boolean hasActiveRide = !rideRepository
                    .findByDriverIdAndStatusIn(
                            driver.getId(),
                            List.of(
                                    Ride.RideStatus.ACCEPTED,
                                    Ride.RideStatus.DRIVER_ARRIVING,
                                    Ride.RideStatus.IN_PROGRESS
                            ))
                    .isEmpty();

            if (hasActiveRide) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "error",
                                "You cannot go offline while you have an active ride."
                        ));
            }
        }

        driver.setAvailable(request.available());
        Driver savedDriver = driverRepository.save(driver);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Availability updated successfully",
                        "driverId", savedDriver.getId(),
                        "available", savedDriver.isAvailable()
                ));
    }

    public record AvailabilityRequest(boolean available) {
    }
}

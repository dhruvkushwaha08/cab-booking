
package com.cabbooking;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver/rides")
public class DriverRideController {

    private static final List<Ride.RideStatus> ACTIVE_RIDE_STATUSES =
            List.of(
                    Ride.RideStatus.ACCEPTED,
                    Ride.RideStatus.DRIVER_ARRIVING,
                    Ride.RideStatus.IN_PROGRESS
            );

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RideStatusEventPublisher rideStatusEventPublisher;

    public DriverRideController(
            RideRepository rideRepository,
            UserRepository userRepository,
            DriverRepository driverRepository,
            RideStatusEventPublisher rideStatusEventPublisher) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.rideStatusEventPublisher = rideStatusEventPublisher;
    }

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableRides(
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (user.getRole() != User.Role.DRIVER) {
            return ResponseEntity.status(403).body(
                    Map.of("error",
                            "Only drivers can view available rides."));
        }

        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver profile not found."));

        if (!driver.isAvailable()) {
            return ResponseEntity.ok(
                    Map.of(
                            "count", 0,
                            "rides", List.of(),
                            "message",
                            "You are currently unavailable for rides."
                    ));
        }

        List<Map<String, Object>> rides = rideRepository
                .findByStatusOrderByRequestedAtAsc(
                        Ride.RideStatus.REQUESTED)
                .stream()
                .map(ride -> Map.<String, Object>of(
                        "rideId", ride.getId(),
                        "pickupLocation", ride.getPickupLocation(),
                        "dropoffLocation", ride.getDropoffLocation(),
                        "distanceKm", ride.getDistanceKm(),
                        "estimatedFare", ride.getEstimatedFare(),
                        "requestedAt", ride.getRequestedAt()
                ))
                .toList();

        return ResponseEntity.ok(
                Map.of("count", rides.size(), "rides", rides));
    }

    @PutMapping("/{rideId}/accept")
    @Transactional
    public ResponseEntity<?> acceptRide(
            @PathVariable Long rideId,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (user.getRole() != User.Role.DRIVER) {
            return ResponseEntity.status(403).body(
                    Map.of("error", "Only drivers can accept rides."));
        }

        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver profile not found."));

        if (!driver.isAvailable()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "You must be online to accept a ride."));
        }

        boolean alreadyHasActiveRide = !rideRepository
                .findByDriverIdAndStatusIn(
                        driver.getId(), ACTIVE_RIDE_STATUSES)
                .isEmpty();

        if (alreadyHasActiveRide) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Complete or cancel your current ride before accepting another."));
        }

        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElse(null);

        if (ride == null) {
            return ResponseEntity.status(404).body(
                    Map.of("error", "Ride not found."));
        }

        if (ride.getStatus() != Ride.RideStatus.REQUESTED) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "This ride is no longer available."));
        }

        ride.setDriver(driver);
        ride.setStatus(Ride.RideStatus.ACCEPTED);
        ride.setAcceptedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.saveAndFlush(ride);

        rideStatusEventPublisher.publish(
                savedRide.getId(),
                savedRide.getStatus().name());

        return ResponseEntity.ok(
                Map.of(
                        "message", "Ride accepted successfully",
                        "rideId", savedRide.getId(),
                        "driverId", driver.getId(),
                        "status", savedRide.getStatus().name()
                ));
    }

    @PutMapping("/{rideId}/start")
    @Transactional
    public ResponseEntity<?> startRide(
            @PathVariable Long rideId,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (user.getRole() != User.Role.DRIVER) {
            return ResponseEntity.status(403).body(
                    Map.of("error", "Only drivers can start rides."));
        }

        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver profile not found."));

        Ride ride = rideRepository.findById(rideId).orElse(null);

        if (ride == null) {
            return ResponseEntity.status(404).body(
                    Map.of("error", "Ride not found."));
        }

        if (ride.getDriver() == null
                || !ride.getDriver().getId().equals(driver.getId())) {
            return ResponseEntity.status(403).body(
                    Map.of("error",
                            "You are not assigned to this ride."));
        }

        if (ride.getStatus() != Ride.RideStatus.ACCEPTED) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Only accepted rides can be started."));
        }

        ride.setStatus(Ride.RideStatus.IN_PROGRESS);
        ride.setStartedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.saveAndFlush(ride);

        rideStatusEventPublisher.publish(
                savedRide.getId(),
                savedRide.getStatus().name());

        return ResponseEntity.ok(
                Map.of(
                        "message", "Ride started successfully",
                        "rideId", savedRide.getId(),
                        "status", savedRide.getStatus().name(),
                        "startedAt", savedRide.getStartedAt().toString()
                ));
    }

    @PutMapping("/{rideId}/complete")
    @Transactional
    public ResponseEntity<?> completeRide(
            @PathVariable Long rideId,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (user.getRole() != User.Role.DRIVER) {
            return ResponseEntity.status(403).body(
                    Map.of("error", "Only drivers can complete rides."));
        }

        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver profile not found."));

        Ride ride = rideRepository.findById(rideId).orElse(null);

        if (ride == null) {
            return ResponseEntity.status(404).body(
                    Map.of("error", "Ride not found."));
        }

        if (ride.getDriver() == null
                || !ride.getDriver().getId().equals(driver.getId())) {
            return ResponseEntity.status(403).body(
                    Map.of("error",
                            "You are not assigned to this ride."));
        }

        if (ride.getStatus() != Ride.RideStatus.IN_PROGRESS) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Only rides in progress can be completed."));
        }

        ride.setStatus(Ride.RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.saveAndFlush(ride);

        rideStatusEventPublisher.publish(
                savedRide.getId(),
                savedRide.getStatus().name());

        return ResponseEntity.ok(
                Map.of(
                        "message", "Ride completed successfully",
                        "rideId", savedRide.getId(),
                        "status", savedRide.getStatus().name(),
                        "completedAt", savedRide.getCompletedAt().toString()
                ));
    }

    @PutMapping("/{rideId}/cancel")
    @Transactional
    public ResponseEntity<?> cancelRide(
            @PathVariable Long rideId,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (user.getRole() != User.Role.DRIVER) {
            return ResponseEntity.status(403).body(
                    Map.of("error", "Only drivers can cancel rides."));
        }

        Driver driver = driverRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver profile not found."));

        Ride ride = rideRepository.findById(rideId).orElse(null);

        if (ride == null) {
            return ResponseEntity.status(404).body(
                    Map.of("error", "Ride not found."));
        }

        if (ride.getDriver() == null
                || !ride.getDriver().getId().equals(driver.getId())) {
            return ResponseEntity.status(403).body(
                    Map.of("error",
                            "You are not assigned to this ride."));
        }

        if (ride.getStatus() != Ride.RideStatus.ACCEPTED) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Only accepted rides can be cancelled by the driver."));
        }

        ride.setStatus(Ride.RideStatus.CANCELLED);

        Ride savedRide = rideRepository.saveAndFlush(ride);

        rideStatusEventPublisher.publish(
                savedRide.getId(),
                savedRide.getStatus().name());

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Ride cancelled successfully by driver",
                        "rideId", savedRide.getId(),
                        "status", savedRide.getStatus().name()
                ));
    }
}

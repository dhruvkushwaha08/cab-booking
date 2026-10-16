
package com.cabbooking;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;
    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final RideStatusEventPublisher rideStatusEventPublisher;

    public RideController(
            RideService rideService,
            UserRepository userRepository,
            RideRepository rideRepository,
            RideStatusEventPublisher rideStatusEventPublisher) {
        this.rideService = rideService;
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.rideStatusEventPublisher = rideStatusEventPublisher;
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookRide(
            @Valid @RequestBody RideRequest request,
            Authentication authentication) {

        User customer = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (customer.getRole() != User.Role.CUSTOMER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("error", "Only customers can book rides.")
            );
        }

        Ride ride = rideService.bookRide(customer.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", "Ride booked successfully",
                        "rideId", ride.getId(),
                        "pickupLocation", ride.getPickupLocation(),
                        "dropoffLocation", ride.getDropoffLocation(),
                        "distanceKm", ride.getDistanceKm(),
                        "estimatedDurationMinutes",
                        ride.getEstimatedDurationMinutes(),
                        "estimatedFare", ride.getEstimatedFare(),
                        "status", ride.getStatus().name()
                )
        );
    }

    @PutMapping("/{rideId}/cancel")
    @Transactional
    public ResponseEntity<?> cancelRide(
            @PathVariable Long rideId,
            Authentication authentication) {

        User customer = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (customer.getRole() != User.Role.CUSTOMER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("error", "Only customers can cancel rides.")
            );
        }

        Ride ride = rideRepository.findById(rideId).orElse(null);

        if (ride == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Ride not found.")
            );
        }

        if (ride.getCustomer() == null
                || !ride.getCustomer().getId().equals(customer.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("error", "You can only cancel your own rides.")
            );
        }

        if (ride.getStatus() != Ride.RideStatus.REQUESTED
                && ride.getStatus() != Ride.RideStatus.ACCEPTED) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error",
                            "Only requested or accepted rides can be cancelled."
                    )
            );
        }

        ride.setStatus(Ride.RideStatus.CANCELLED);

        Ride savedRide = rideRepository.saveAndFlush(ride);

        rideStatusEventPublisher.publish(
                savedRide.getId(),
                savedRide.getStatus().name()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Ride cancelled successfully",
                        "rideId", savedRide.getId(),
                        "status", savedRide.getStatus().name()
                )
        );
    }
}


package com.cabbooking;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rides")
public class RideHistoryController {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    public RideHistoryController(
            RideRepository rideRepository,
            UserRepository userRepository) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/history")
    public ResponseEntity<?> getRideHistory(
            Authentication authentication) {

        String email = authentication.getName();

        User customer = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (customer.getRole() != User.Role.CUSTOMER) {
            return ResponseEntity.status(403).body(
                    Map.of("error",
                            "Only customers can view customer ride history.")
            );
        }

        List<Map<String, Object>> rides = new ArrayList<>();

        rideRepository
                .findByCustomerIdOrderByRequestedAtDesc(customer.getId())
                .forEach(ride -> {
                    Map<String, Object> rideData = new LinkedHashMap<>();
                    rideData.put("rideId", ride.getId());
                    rideData.put("pickupLocation", ride.getPickupLocation());
                    rideData.put("dropoffLocation", ride.getDropoffLocation());
                    rideData.put("distanceKm", ride.getDistanceKm());
                    rideData.put(
                            "estimatedDurationMinutes",
                            ride.getEstimatedDurationMinutes());
                    rideData.put("estimatedFare", ride.getEstimatedFare());
                    rideData.put("status", ride.getStatus().name());
                    rideData.put("requestedAt", ride.getRequestedAt());
                    rides.add(rideData);
                });

        return ResponseEntity.ok(
                Map.of(
                        "count", rides.size(),
                        "rides", rides
                )
        );
    }
}


package com.cabbooking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final RideStatusEventPublisher rideStatusEventPublisher;
    private final RoutingService routingService;

    public RideService(
            RideRepository rideRepository,
            UserRepository userRepository,
            RideStatusEventPublisher rideStatusEventPublisher,
            RoutingService routingService) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.rideStatusEventPublisher = rideStatusEventPublisher;
        this.routingService = routingService;
    }

    @Transactional
    public Ride bookRide(Long customerId, RideRequest request) {

        User customer = userRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Customer not found."));

        if (customer.getRole() != User.Role.CUSTOMER) {
            throw new IllegalArgumentException(
                    "Only customers can book rides.");
        }

        RoutingService.RouteResult route = routingService.getRoute(
                request.pickupLongitude(),
                request.pickupLatitude(),
                request.dropoffLongitude(),
                request.dropoffLatitude()
        );

        double distanceKm = route.distanceKm();

        BigDecimal estimatedDurationMinutes =
                BigDecimal.valueOf(route.durationMinutes())
                        .setScale(2, RoundingMode.HALF_UP);

        // Demo fare: base fare + per-kilometre charge.
        BigDecimal estimatedFare = BigDecimal.valueOf(
                50.0 + distanceKm * 12.0
        ).setScale(2, RoundingMode.HALF_UP);

        Ride ride = new Ride();
        ride.setCustomer(customer);
        ride.setPickupLocation(request.pickupLocation().trim());
        ride.setDropoffLocation(request.dropoffLocation().trim());
        ride.setPickupLatitude(request.pickupLatitude());
        ride.setPickupLongitude(request.pickupLongitude());
        ride.setDropoffLatitude(request.dropoffLatitude());
        ride.setDropoffLongitude(request.dropoffLongitude());

        ride.setDistanceKm(BigDecimal.valueOf(distanceKm)
                .setScale(2, RoundingMode.HALF_UP));
        ride.setEstimatedDurationMinutes(estimatedDurationMinutes);
        ride.setEstimatedFare(estimatedFare);
        ride.setStatus(Ride.RideStatus.REQUESTED);

        Ride savedRide = rideRepository.save(ride);

        rideStatusEventPublisher.publish(
                savedRide.getId(),
                savedRide.getStatus().name()
        );

        return savedRide;
    }
}

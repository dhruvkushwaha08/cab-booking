
package com.cabbooking;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByCustomerIdOrderByRequestedAtDesc(Long customerId);

    List<Ride> findByDriverIdOrderByRequestedAtDesc(Long driverId);

    List<Ride> findByStatusOrderByRequestedAtAsc(
            Ride.RideStatus status
    );

    List<Ride> findByCustomerIdAndStatus(
            Long customerId,
            Ride.RideStatus status
    );

    List<Ride> findByDriverIdAndStatus(
            Long driverId,
            Ride.RideStatus status
    );

    List<Ride> findByDriverIdAndStatusIn(
            Long driverId,
            List<Ride.RideStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Ride r WHERE r.id = :rideId")
    Optional<Ride> findByIdForUpdate(
            @Param("rideId") Long rideId
    );
}

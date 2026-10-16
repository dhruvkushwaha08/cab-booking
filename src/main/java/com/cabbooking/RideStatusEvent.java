
package com.cabbooking;

public record RideStatusEvent(
        Long rideId,
        String status
) {
}

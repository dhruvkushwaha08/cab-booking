
package com.cabbooking;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class RideStatusEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public RideStatusEventPublisher(
            ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(Long rideId, String status) {
        RideStatusEvent event = new RideStatusEvent(rideId, status);
        eventPublisher.publishEvent(event);
    }
}

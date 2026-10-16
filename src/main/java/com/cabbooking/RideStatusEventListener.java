
package com.cabbooking;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class RideStatusEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public RideStatusEventListener(
            SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleRideStatusChange(RideStatusEvent event) {

        Map<String, Object> statusUpdate = Map.of(
                "rideId", event.rideId(),
                "status", event.status(),
                "timestamp", LocalDateTime.now().toString()
        );

        messagingTemplate.convertAndSend(
                "/topic/rides/" + event.rideId() + "/status",
                (Object) statusUpdate
        );
    }
}

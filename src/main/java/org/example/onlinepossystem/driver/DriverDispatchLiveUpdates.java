package org.example.onlinepossystem.driver;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DriverDispatchLiveUpdates {
    private final SimpMessageSendingOperations messaging;

    public DriverDispatchLiveUpdates(SimpMessageSendingOperations messaging) {
        this.messaging = messaging;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("realtimeTaskExecutor")
    public void dispatchChanged(DriverDispatchChanged event) {
        messaging.convertAndSend("/topic/admin/branches/" + event.branchId() + "/dispatch", event);
        messaging.convertAndSend("/topic/admin/dispatch", event);
        if (event.driverUsername() != null) {
            messaging.convertAndSendToUser(event.driverUsername(), "/queue/driver", event);
        }
    }
}

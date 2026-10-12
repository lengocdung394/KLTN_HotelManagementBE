package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerSocketEmitter {
    private final SocketIOServer socketIOServer;
    private static final String SUPER_ADMIN_ROOM = "super_admin_accounts";

    public void emitEmployeeCreated(Long hotelId, EmployeeResponse employee) {
        emitEmployeeChanged("employee_created", hotelId, employee);
    }

    public void emitEmployeeUpdated(Long previousHotelId, Long currentHotelId, EmployeeResponse employee) {
        emitEmployeeChanged("employee_updated", previousHotelId, employee);
        if (currentHotelId != null && !currentHotelId.equals(previousHotelId)) {
            emitEmployeeChanged("employee_updated", currentHotelId, employee);
        }
    }

    private void emitEmployeeChanged(String event, Long hotelId, EmployeeResponse employee) {
        EmployeeChangedPayload payload = new EmployeeChangedPayload(
                hotelId, employee, Instant.now());
        afterCommit(() -> {
            try {
                if (hotelId != null) {
                    socketIOServer.getRoomOperations("hotel_" + hotelId).sendEvent(event, payload);
                }
                socketIOServer.getRoomOperations(SUPER_ADMIN_ROOM).sendEvent(event, payload);
                log.info("Emitted {} for employee {} at hotel {}", event, employee.getId(), hotelId);
            } catch (RuntimeException exception) {
                log.error("Could not emit {} for employee {}", event, employee.getId(), exception);
            }
        });
    }

    private void afterCommit(Runnable emit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emit.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emit.run();
            }
        });
    }

    private record EmployeeChangedPayload(Long hotelId, EmployeeResponse employee, Instant occurredAt) {
    }
}

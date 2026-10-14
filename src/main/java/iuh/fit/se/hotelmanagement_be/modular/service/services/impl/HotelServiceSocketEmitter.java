package iuh.fit.se.hotelmanagement_be.modular.service.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.service.responses.ServiceResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class HotelServiceSocketEmitter {
    SocketIOServer socketIOServer;

    public void emitServiceCreated(ServiceResponse service, List<Long> targetHotelIds) {
        List<Long> branches = targetHotelIds == null ? List.of() : targetHotelIds.stream().distinct().toList();
        publishAfterCommit(() -> {
            try {
                Map<String, Object> payload = Map.of(
                        "service", service,
                        "name", service.getName(),
                        "serviceId", service.getId(),
                        "hotelName", service.getHotelName()
                );
                branches.forEach(branchId -> socketIOServer.getRoomOperations("hotel_" + branchId)
                        .sendEvent("new_service_notification", payload));
                socketIOServer.getRoomOperations("super_admin_accounts")
                        .sendEvent("new_service_notification", payload);
                log.info("Service {} creation emitted to {} branch rooms", service.getId(), branches.size());
            } catch (RuntimeException exception) {
                log.error("Could not emit service creation {}", service.getId(), exception);
            }
        });
    }

    public void emitServiceUpdated(
            ServiceResponse service,
            Map<String, String> changes,
            List<Long> targetHotelIds) {
        List<Long> branches = targetHotelIds == null ? List.of() : targetHotelIds.stream().distinct().toList();
        publishAfterCommit(() -> {
            try {
                Map<String, Object> payload = Map.of(
                        "service", service,
                        "serviceId", service.getId(),
                        "serviceName", service.getName(),
                        "changes", changes
                );
                branches.forEach(branchId -> socketIOServer.getRoomOperations("hotel_" + branchId)
                        .sendEvent("update_service_notification", payload));
                socketIOServer.getRoomOperations("super_admin_accounts")
                        .sendEvent("update_service_notification", payload);
                log.info("Service {} update emitted to {} branch rooms", service.getId(), branches.size());
            } catch (RuntimeException exception) {
                log.error("Could not emit service update {}", service.getId(), exception);
            }
        });
    }

    private void publishAfterCommit(Runnable emit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emit.run();
                }
            });
        } else {
            emit.run();
        }
    }
}

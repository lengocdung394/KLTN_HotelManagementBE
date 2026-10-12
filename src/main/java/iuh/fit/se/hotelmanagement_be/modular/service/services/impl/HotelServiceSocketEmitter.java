package iuh.fit.se.hotelmanagement_be.modular.service.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class HotelServiceSocketEmitter {
    SocketIOServer socketIOServer;
    NotificationService notificationService;

    public void emitServiceCreated(Long hotelId, String serviceId, String serviceName) {
        if (hotelId == null) return;

        notificationService.createAndPublish(
                hotelId,
                NotificationType.SERVICE_CREATED,
                "Dịch vụ mới được thêm",
                "Dịch vụ \"" + serviceName + "\" đã được thêm.",
                "SERVICE",
                serviceId
        );

        String roomName = "hotel_" + hotelId;
        socketIOServer.getRoomOperations(roomName)
                .sendEvent("new_service_notification", serviceName);
        socketIOServer.getRoomOperations("super_admin_accounts")
                .sendEvent(
                        "new_service_notification",
                        Map.of("hotelId", hotelId, "data", serviceName)
                );

        log.info("Đã gửi thông báo dịch vụ mới tới room {} và super_admin_accounts", roomName);
    }

    public void emitServiceUpdated(
            Long hotelId,
            String serviceId,
            String serviceName,
            Map<String, String> changes
    ) {
        if (hotelId == null) return;

        String changeDetails = changes.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("; "));

        String description = "Dịch vụ \"" + serviceName + "\" đã được chỉnh sửa"
                + (changeDetails.isBlank() ? "." : ": " + changeDetails + ".");

        notificationService.createAndPublish(
                hotelId,
                NotificationType.SERVICE_UPDATED,
                "Dịch vụ đã được cập nhật",
                description,
                "SERVICE",
                serviceId
        );

        Map<String, Object> payload = Map.of(
                "hotelId", hotelId,
                "serviceId", serviceId,
                "serviceName", serviceName,
                "changes", changes
        );

        socketIOServer.getRoomOperations("hotel_" + hotelId)
                .sendEvent("update_service_notification", payload);
        socketIOServer.getRoomOperations("super_admin_accounts")
                .sendEvent("update_service_notification", payload);
    }
}
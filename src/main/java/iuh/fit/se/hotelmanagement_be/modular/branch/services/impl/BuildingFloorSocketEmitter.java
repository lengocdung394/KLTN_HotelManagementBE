package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BuildingFloorSocketEmitter {

    SocketIOServer socketIOServer;
    NotificationService notificationService;

    public void emitBuildingChanged(
            Long hotelId,
            String action,
            Object building
    ) {
        emitBuildingChanged(hotelId, action, building, false);
    }

    public void emitBuildingChanged(
            Long hotelId,
            String action,
            Object building,
            boolean isSuperAdmin
    ) {
        boolean created = isCreateAction(action);

        emit(
                hotelId,
                "building_changed",
                "BUILDING",
                action,
                building,
                created
                        ? NotificationType.BUILDING_CREATED
                        : NotificationType.BUILDING_UPDATED,
                created ? "Tòa nhà mới" : "Tòa nhà được cập nhật",
                created
                        ? "Một tòa nhà mới vừa được thêm vào chi nhánh."
                        : "Thông tin tòa nhà tại chi nhánh vừa được cập nhật.",
                isSuperAdmin
        );
    }

    public void emitFloorChanged(
            Long hotelId,
            String action,
            Object floor
    ) {
        emitFloorChanged(hotelId, action, floor, false);
    }

    public void emitFloorChanged(
            Long hotelId,
            String action,
            Object floor,
            boolean isSuperAdmin
    ) {
        boolean created = isCreateAction(action);

        emit(
                hotelId,
                "floor_changed",
                "FLOOR",
                action,
                floor,
                created
                        ? NotificationType.FLOOR_CREATED
                        : NotificationType.FLOOR_UPDATED,
                created ? "Tầng mới" : "Tầng được cập nhật",
                created
                        ? "Một tầng mới vừa được thêm vào tòa nhà."
                        : "Thông tin tầng trong tòa nhà vừa được cập nhật.",
                isSuperAdmin
        );
    }

    private void emit(
            Long hotelId,
            String event,
            String entityType,
            String action,
            Object data,
            NotificationType notificationType,
            String title,
            String fallbackDescription,
            boolean isSuperAdmin
    ) {
        if (hotelId == null) {
            log.warn("Cannot emit {} without hotelId", event);
            return;
        }

        boolean updated = isUpdateAction(action);

        // Khi cập nhật, chỉ gửi tới bên không thực hiện thay đổi.
        // Các action khác giữ hành vi hiện tại và gửi tới cả hai bên.
        boolean publishToHotel = !updated || isSuperAdmin;
        boolean publishToSuperAdmin = !updated || !isSuperAdmin;

        String description = buildChangeDescription(
                entityType,
                action,
                data,
                fallbackDescription
        );
        log.info("Notification debug: event={}, action={}, data={}, description={}",
                event, action, data, description);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelId", hotelId);
        payload.put("entityType", entityType);
        payload.put("action", action);
        payload.put("data", data);
        payload.put("occurredAt", Instant.now().toString());

        try {
            notificationService.createAndPublish(
                    hotelId,
                    notificationType,
                    title,
                    description,
                    entityType,
                    null,
                    publishToHotel,
                    publishToSuperAdmin
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Could not persist {} notification for hotel {}",
                    entityType,
                    hotelId,
                    exception
            );
        }

        try {
            if (publishToHotel) {
                socketIOServer.getRoomOperations("hotel_" + hotelId)
                        .sendEvent(event, payload);
            }

            if (publishToSuperAdmin) {
                socketIOServer.getRoomOperations("super_admin_accounts")
                        .sendEvent(event, payload);
            }

            log.info(
                    "Emitted {} ({}) for hotel {}; hotelRoom={}, superAdminRoom={}",
                    event,
                    action,
                    hotelId,
                    publishToHotel,
                    publishToSuperAdmin
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Could not emit {} ({}) for hotel {}",
                    event,
                    action,
                    hotelId,
                    exception
            );
        }
    }

    private String buildChangeDescription(
            String entityType,
            String action,
            Object data,
            String fallback
    ) {
        if (!(data instanceof Map<?, ?> dataMap)
                || !(dataMap.get("changes") instanceof List<?> changes)
                || changes.isEmpty()) {
            return fallback;
        }

        List<String> details = new ArrayList<>();

        for (Object item : changes) {
            if (!(item instanceof Map<?, ?> change)) {
                continue;
            }

            Object label = change.get("label");
            Object before = change.get("before");
            Object after = change.get("after");

            if (label == null || before == null || after == null) {
                continue;
            }

            details.add(
                    label + ": "
                            + displayValue(before)
                            + " → "
                            + displayValue(after)
            );
        }

        if (details.isEmpty()) {
            return fallback;
        }

        String subject = getSubject(entityType, dataMap);
        String actionText = isCreateAction(action)
                ? "được thêm mới"
                : "được cập nhật";

        return subject + " " + actionText + ": "
                + details.stream().collect(Collectors.joining("; "))
                + ".";
    }

    private String getSubject(String entityType, Map<?, ?> data) {
        if ("BUILDING".equals(entityType)) {
            Object name = data.get("name");
            return name == null ? "Tòa nhà" : "Tòa nhà " + name;
        }

        if ("FLOOR".equals(entityType)) {
            Object floorNumber = data.get("floorNumber");
            return floorNumber == null ? "Tầng" : "Tầng " + floorNumber;
        }

        return "Thông tin";
    }

    private String displayValue(Object value) {
        return value == null ? "—" : String.valueOf(value);
    }

    private boolean isCreateAction(String action) {
        return "CREATE".equalsIgnoreCase(action)
                || "CREATED".equalsIgnoreCase(action);
    }

    private boolean isUpdateAction(String action) {
        return "UPDATE".equalsIgnoreCase(action)
                || "UPDATED".equalsIgnoreCase(action);
    }
}
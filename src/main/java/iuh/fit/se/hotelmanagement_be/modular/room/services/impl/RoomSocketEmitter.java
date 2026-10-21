package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomSocketEmitter {

    private static final String SUPER_ADMIN_ROOM = "super_admin_accounts";

    private final SocketIOServer socketIOServer;
    private final NotificationService notificationService;

    public void emitRoomCreated(Long hotelId, Object roomData) {
        emitRoomChanged(hotelId, "CREATED", roomData, false);
    }

    public void emitRoomUpdated(Long hotelId, Object roomData) {
        emitRoomUpdated(hotelId, roomData, false);
    }

    public void emitRoomUpdated(
            Long hotelId,
            Object roomData,
            boolean isSuperAdmin
    ) {
        emitRoomChanged(hotelId, "UPDATED", roomData, isSuperAdmin);
    }

    public void emitRoomsImported(Long hotelId, int count) {
        emitRoomChanged(
                hotelId,
                "IMPORTED",
                Map.of("count", count),
                false
        );
    }

    public void emitRoomStatusUpdated(Long hotelId, String roomId, String roomNumber, RoomStatus roomStatus) {
        if (hotelId == null) return;

        Map<String, Object> roomData = new LinkedHashMap<>();
        roomData.put("roomId", roomId);
        roomData.put("roomNumber", roomNumber);
        roomData.put("roomStatus", roomStatus);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelId", hotelId);
        payload.put("action", "STATUS_UPDATED");
        payload.put("data", roomData);
        payload.put("occurredAt", Instant.now().toString());

        Runnable emit = () -> {
            try {
                socketIOServer.getRoomOperations("hotel_" + hotelId).sendEvent("room_update", payload);
                socketIOServer.getRoomOperations(SUPER_ADMIN_ROOM).sendEvent("room_update", payload);
            } catch (RuntimeException exception) {
                log.error("Could not emit room status update for room {}", roomId, exception);
            }
        };
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

    private void emitRoomChanged(
            Long hotelId,
            String action,
            Object roomData,
            boolean isSuperAdmin
    ) {
        if (hotelId == null) {
            log.warn("Cannot emit room change event without hotelId");
            return;
        }

        boolean created = "CREATE".equalsIgnoreCase(action)
                || "CREATED".equalsIgnoreCase(action);
        boolean updated = isUpdateAction(action);

        // Cập nhật chỉ gửi tới bên còn lại.
        // Tạo/import phòng là thao tác của chi nhánh, chỉ gửi tới chi nhánh.
        boolean publishToHotel = !updated || isSuperAdmin;
        boolean publishToSuperAdmin = updated && !isSuperAdmin;

        String event = updated ? "room_update" : "room_create";

        if (created || updated) {
            NotificationType type = created
                    ? NotificationType.ROOM_CREATED
                    : NotificationType.ROOM_UPDATED;

            String referenceId = getRoomReferenceId(roomData);
            String description = created
                    ? "Một phòng mới vừa được thêm vào chi nhánh."
                    : buildRoomUpdateDescription(roomData);

            notificationService.createAndPublish(
                    hotelId,
                    type,
                    created ? "Phòng mới" : "Phòng được cập nhật",
                    description,
                    "ROOM",
                    referenceId,
                    publishToHotel,
                    publishToSuperAdmin
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelId", hotelId);
        payload.put("action", action);
        payload.put("data", roomData);
        payload.put("occurredAt", Instant.now().toString());

        Runnable emit = () -> {
            try {
                if (publishToHotel) {
                    socketIOServer.getRoomOperations("hotel_" + hotelId)
                            .sendEvent(event, payload);
                }

                if (publishToSuperAdmin) {
                    socketIOServer.getRoomOperations(SUPER_ADMIN_ROOM)
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
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            emit.run();
                        }
                    }
            );
        } else {
            emit.run();
        }
    }

    private boolean isUpdateAction(String action) {
        return "UPDATE".equalsIgnoreCase(action)
                || "UPDATED".equalsIgnoreCase(action);
    }

    private String getRoomReferenceId(Object roomData) {
        if (roomData instanceof Map<?, ?> data) {
            Object id = data.get("roomId");

            if (id == null) {
                id = data.get("id");
            }

            return id == null ? null : String.valueOf(id);
        }

        return null;
    }

    private String buildRoomUpdateDescription(Object roomData) {
        if (!(roomData instanceof Map<?, ?> data)) {
            return "Thông tin một phòng tại chi nhánh vừa được cập nhật.";
        }

        Object roomNumber = data.get("roomNumber");
        Object rawChanges = data.get("changes");

        if (!(rawChanges instanceof List<?> changes) || changes.isEmpty()) {
            return "Phòng "
                    + (roomNumber == null ? "" : roomNumber + " ")
                    + "vừa được chỉnh sửa.";
        }

        String details = changes.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(change -> {
                    Object label = change.get("label");
                    Object before = change.get("before");
                    Object after = change.get("after");

                    if (label == null || before == null || after == null) {
                        return "";
                    }

                    return label + ": " + before + " → " + after;
                })
                .filter(description -> !description.isBlank())
                .collect(Collectors.joining("; "));

        if (details.isBlank()) {
            return "Phòng "
                    + (roomNumber == null ? "" : roomNumber + " ")
                    + "vừa được chỉnh sửa.";
        }

        return "Phòng "
                + (roomNumber == null ? "" : roomNumber + " ")
                + "được chỉnh sửa: "
                + details
                + ".";
    }
}
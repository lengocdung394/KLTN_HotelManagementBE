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
public class BranchSocketEmitter {

    SocketIOServer socketIOServer;
    NotificationService notificationService;

    public void emitRoomPolicyUpdate(Long hotelId, Object policyData) {
        emitRoomPolicyChanged(hotelId, "UPDATED", policyData, false);
    }

    public void emitRoomPolicyUpdate(
            Long hotelId,
            Object policyData,
            boolean isSuperAdmin
    ) {
        emitRoomPolicyChanged(hotelId, "UPDATED", policyData, isSuperAdmin);
    }

    public void emitRoomPolicyChanged(
            Long hotelId,
            String action,
            Object policyData
    ) {
        emitRoomPolicyChanged(hotelId, action, policyData, false);
    }

    public void emitRoomPolicyChanged(
            Long hotelId,
            String action,
            Object policyData,
            boolean isSuperAdmin
    ) {
        if (hotelId == null) {
            log.warn("Cannot emit room policy event without hotelId");
            return;
        }

        boolean created = isCreateAction(action);
        boolean updated = isUpdateAction(action);

        // Khi cập nhật, chỉ gửi tới bên không thực hiện thay đổi.
        // Khi tạo mới, gửi tới cả chi nhánh và Super Admin.
        boolean publishToHotel = !updated || isSuperAdmin;
        boolean publishToSuperAdmin = !updated || !isSuperAdmin;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelId", hotelId);
        payload.put("action", action);
        payload.put("data", policyData);
        payload.put("occurredAt", Instant.now().toString());

        NotificationType notificationType = created
                ? NotificationType.ROOM_POLICY_CREATED
                : NotificationType.ROOM_POLICY_UPDATED;

        String title = created
                ? "Chính sách phòng mới"
                : "Chính sách phòng được cập nhật";

        String roomType = getValue(policyData, "roomType", "phòng");
        String fallbackDescription = created
                ? "Cấu hình giá và sức chứa phòng mới vừa được thêm vào chi nhánh."
                : "Cấu hình giá hoặc sức chứa phòng tại chi nhánh vừa được cập nhật.";

        String description = buildChangeDescription(
                "Cấu hình " + roomType,
                action,
                policyData,
                fallbackDescription
        );

        try {
            notificationService.createAndPublish(
                    hotelId,
                    notificationType,
                    title,
                    description,
                    "ROOM_POLICY",
                    null,
                    publishToHotel,
                    publishToSuperAdmin
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Could not persist room policy notification for hotel {}",
                    hotelId,
                    exception
            );
        }

        try {
            socketIOServer.getRoomOperations("hotel_" + hotelId)
                    .sendEvent("room_policy_updated", payload);

            if (publishToSuperAdmin) {
                socketIOServer.getRoomOperations("super_admin_accounts")
                        .sendEvent("room_policy_updated", payload);
            }

            log.info(
                    "Emitted room_policy_updated ({}) for hotel {}; hotelRoom={}, superAdminRoom={}",
                    action,
                    hotelId,
                    publishToHotel,
                    publishToSuperAdmin
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Could not emit room_policy_updated ({}) for hotel {}",
                    action,
                    hotelId,
                    exception
            );
        }
    }

    private String buildChangeDescription(
            String subject,
            String action,
            Object data,
            String fallback
    ) {
        if (!(data instanceof Map<?, ?> map)
                || !(map.get("changes") instanceof List<?> changes)
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

        String actionText = isCreateAction(action)
                ? "được thêm mới"
                : "được cập nhật";

        return subject + " " + actionText + ": "
                + details.stream().collect(Collectors.joining("; "))
                + ".";
    }

    private String displayValue(Object value) {
        return value == null ? "—" : String.valueOf(value);
    }

    private String getValue(Object data, String key, String fallback) {
        if (data instanceof Map<?, ?> map && map.get(key) != null) {
            return String.valueOf(map.get(key));
        }

        return fallback;
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
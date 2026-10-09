package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuildingFloorSocketEmitter {
    private final SocketIOServer socketIOServer;

    public void emitBuildingChanged(Long hotelId, String action, Object building) {
        emit(hotelId, "building_changed", "BUILDING", action, building);
    }

    public void emitFloorChanged(Long hotelId, String action, Object floor) {
        emit(hotelId, "floor_changed", "FLOOR", action, floor);
    }

    private void emit(Long hotelId, String event, String entityType, String action, Object data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelId", hotelId);
        payload.put("entityType", entityType);
        payload.put("action", action);
        payload.put("data", data);
        payload.put("occurredAt", Instant.now().toString());

        try {
            socketIOServer.getRoomOperations("hotel_" + hotelId).sendEvent(event, payload);
            socketIOServer.getRoomOperations("super_admin_accounts").sendEvent(event, payload);
            log.info("Emitted {} ({}) for hotel {}", event, action, hotelId);
        } catch (RuntimeException exception) {
            log.error("Could not emit {} ({}) for hotel {}", event, action, hotelId, exception);
        }
    }
}

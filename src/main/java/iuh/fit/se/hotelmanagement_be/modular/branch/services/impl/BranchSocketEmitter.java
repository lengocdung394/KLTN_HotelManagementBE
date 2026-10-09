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
public class BranchSocketEmitter {
    private final SocketIOServer socketIOServer;

    public void emitRoomPolicyUpdate(Long hotelId, Object policyData) {
        emitRoomPolicyChanged(hotelId, "UPDATED", policyData);
    }

    public void emitRoomPolicyChanged(Long hotelId, String action, Object policyData) {
        if (hotelId == null) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelId", hotelId);
        payload.put("action", action);
        payload.put("data", policyData);
        payload.put("occurredAt", Instant.now().toString());

        try {
            socketIOServer.getRoomOperations("hotel_" + hotelId)
                    .sendEvent("room_policy_updated", payload);
            socketIOServer.getRoomOperations("super_admin_accounts")
                    .sendEvent("room_policy_updated", payload);
            log.info("Emitted room_policy_updated ({}) for hotel {}", action, hotelId);
        } catch (RuntimeException exception) {
            log.error("Could not emit room_policy_updated ({}) for hotel {}", action, hotelId, exception);
        }
    }
}

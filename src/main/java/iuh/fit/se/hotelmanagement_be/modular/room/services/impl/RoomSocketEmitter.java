package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomSocketEmitter {
    private final SocketIOServer socketIOServer;

    private static final String SUPER_ADMIN_ROOM = "super_admin_accounts";

    public void emitRoomCreated(Long hotelId, Object roomData) {
        emitRoomChanged(hotelId, "CREATED", roomData);
    }

    public void emitRoomUpdated(Long hotelId, Object roomData) {
        emitRoomChanged(hotelId, "UPDATED", roomData);
    }

    public void emitRoomsImported(Long hotelId, int count) {
        emitRoomChanged(hotelId, "IMPORTED", Map.of("count", count));
    }

    void emitRoomChanged(Long hotelId, String action, Object roomData) {
        if (hotelId == null) {
            log.warn("Cannot emit room change event without hotelId");
            return;
        }
        String event = "UPDATED".equals(action) ? "room_update" : "room_create";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelId", hotelId);
        payload.put("action", action);
        payload.put("data", roomData);
        payload.put("occurredAt", Instant.now().toString());

        Runnable emit = () -> {
            try {
                socketIOServer.getRoomOperations("hotel_" + hotelId).sendEvent(event, payload);
                socketIOServer.getRoomOperations(SUPER_ADMIN_ROOM).sendEvent(event, payload);
                log.info("Emitted {} ({}) to hotel_{} and {}", event, action, hotelId, SUPER_ADMIN_ROOM);
            } catch (RuntimeException exception) {
                log.error("Could not emit {} ({}) for hotel {}", event, action, hotelId, exception);
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
}

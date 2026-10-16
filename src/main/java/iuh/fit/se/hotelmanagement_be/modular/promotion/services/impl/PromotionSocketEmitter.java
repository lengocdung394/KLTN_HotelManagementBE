package iuh.fit.se.hotelmanagement_be.modular.promotion.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionSocketEmitter {

    private final SocketIOServer socketIOServer;

    /**
     * 1. Bắn sự kiện cập nhật Lịch tổng phòng (Room Matrix) cho nhân viên chi nhánh
     */
    public void emitPromotionUpdate(Long hotelId, Object promotion) {
        if (hotelId != null) {
            publishAfterCommit(() -> {
                try {
                    String roomName = "hotel_" + hotelId;
                    socketIOServer.getRoomOperations(roomName)
                            .sendEvent("update_promotion_notification", promotion);
                    log.info("Promotion update sent to branch room {}", roomName);
                } catch (RuntimeException exception) {
                    log.error("Could not emit promotion update for hotel {}", hotelId, exception);
                }
            });
        }
    }

    public void emitSystemPromotionUpdate(List<Long> hotelIds, Object promotion) {
        List<Long> targetHotelIds = hotelIds == null ? List.of() : hotelIds.stream().distinct().toList();
        publishAfterCommit(() -> {
            try {
                targetHotelIds.forEach(hotelId -> socketIOServer
                        .getRoomOperations("hotel_" + hotelId)
                        .sendEvent("update_promotion_notification", promotion));
                socketIOServer.getRoomOperations("super_admin_accounts")
                        .sendEvent("update_promotion_notification", promotion);
                log.info("System-wide promotion update sent to {} branch rooms", targetHotelIds.size());
            } catch (RuntimeException exception) {
                log.error("Could not emit system-wide promotion update", exception);
            }
        });
    }

    /**
     * 2. Gửi thông báo chi tiết (Notification) có đơn mới cho nhân viên chi nhánh
     */
    public void emitPromotionCreate(Long hotelId, Object promotion) {
        if (hotelId != null) {
            publishAfterCommit(() -> {
                try {
                    String roomName = "hotel_" + hotelId;
                    socketIOServer.getRoomOperations(roomName)
                            .sendEvent("new_promotion_notification", promotion);
                    socketIOServer.getRoomOperations(roomName)
                            .sendEvent("update_promotion_notification", promotion);
                    log.info("🔔 [Promotion Socket] Đã phát sự kiện khuyến mãi mới tới chi nhánh: {}", roomName);
                } catch (RuntimeException exception) {
                    log.error("Could not emit promotion creation for hotel {}", hotelId, exception);
                }
            });
        }
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

    public void emitSystemPromotionCreate(List<Long> hotelIds, Object promotion) {
        List<Long> targetHotelIds = hotelIds == null ? List.of() : hotelIds.stream().distinct().toList();
        Runnable emit = () -> {
            try {
                targetHotelIds.forEach(hotelId -> {
                    String roomName = "hotel_" + hotelId;
                    socketIOServer.getRoomOperations(roomName)
                            .sendEvent("new_promotion_notification", promotion);
                    socketIOServer.getRoomOperations(roomName)
                            .sendEvent("update_promotion_notification", promotion);
                });
                socketIOServer.getRoomOperations("super_admin_accounts")
                        .sendEvent("new_promotion_notification", promotion);
                log.info("System-wide promotion sent to {} branch rooms", targetHotelIds.size());
            } catch (RuntimeException exception) {
                log.error("Could not emit system-wide promotion to all branches", exception);
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

    // phan tich luong khuyen mai cho khach hàng nha


}

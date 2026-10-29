package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.LateCheckOutBookingNotificationResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingModificationRequest;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingSocketEmitter {
    private final SocketIOServer socketIOServer;
    private final NotificationService notificationService;

    /**
     * 1. Bắn sự kiện cập nhật Lịch tổng phòng (Room Matrix) cho nhân viên chi nhánh
     */
    public void emitRoomMatrixUpdate(Long hotelId) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            Runnable emit = () -> {
                try {
                    socketIOServer.getRoomOperations(roomName)
                            .sendEvent("room_matrix_updated", "Lịch tổng phòng vừa được làm mới do có booking mới!");
                    log.info("🏢 [Booking Socket] Đã gửi 'room_matrix_updated' tới room chi nhánh: {}", roomName);
                } catch (RuntimeException exception) {
                    log.error("Không thể gửi cập nhật lịch phòng tới chi nhánh {}", roomName, exception);
                }
            };

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
    }

    public void emitHousekeepingTaskUpdate(Long hotelId) {
        if (hotelId == null) return;

        Runnable emit = () -> {
            try {
                socketIOServer.getRoomOperations("hotel_" + hotelId)
                        .sendEvent("housekeeping_task_updated", Map.of("hotelId", hotelId));
                log.info("Emitted housekeeping_task_updated for hotel {}", hotelId);
            } catch (RuntimeException exception) {
                log.error("Could not emit housekeeping task update for hotel {}", hotelId, exception);
            }
        };

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

    public void emitBookingStateUpdated(Long hotelId, String bookingId, String action) {
        if (hotelId == null || bookingId == null) return;

        Map<String, Object> payload = Map.of(
                "hotelId", hotelId,
                "bookingId", bookingId,
                "action", action
        );
        Runnable emit = () -> {
            try {
                socketIOServer.getRoomOperations("hotel_" + hotelId)
                        .sendEvent("booking_state_updated", payload);
                log.info("Emitted booking_state_updated for booking {} at hotel {}", bookingId, hotelId);
            } catch (RuntimeException exception) {
                log.error("Could not emit booking_state_updated for booking {}", bookingId, exception);
            }
        };

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

    /**
     * 2. Gửi thông báo chi tiết (Notification) có đơn mới cho nhân viên chi nhánh
     */
    public void emitNewBookingNotification(
            Long hotelId,
            Booking booking,
            Object bookingData
    ) {
        if (hotelId == null || booking == null) return;

        Customer customer = booking.getCustomer();
        String customerName = customer != null && customer.getFullName() != null
                ? customer.getFullName()
                : "Chưa cập nhật";
        String customerPhone = customer != null && customer.getPhone() != null
                ? customer.getPhone()
                : "Chưa cập nhật";
        BigDecimal totalAmount = booking.getOrder() != null && booking.getOrder().getTotalAmount() != null
                ? booking.getOrder().getTotalAmount()
                : BigDecimal.ZERO;
        String formattedAmount = formatAmount(totalAmount);

        notificationService.createAndPublishInCurrentTransaction(
                hotelId,
                NotificationType.BOOKING_CREATED,
                "Booking mới",
                "Mã booking: #" + booking.getId()
                        + " | Khách hàng: " + customerName
                        + " | SĐT: " + customerPhone
                        + " | Tổng tiền booking: " + formattedAmount + "đ.",
                "BOOKING",
                booking.getId()
        );

        String roomName = "hotel_" + hotelId;
        Runnable emit = () -> {
            try {
                socketIOServer.getRoomOperations(roomName)
                        .sendEvent("new_booking_notification", bookingData);
                socketIOServer.getRoomOperations("super_admin_accounts")
                        .sendEvent("new_booking_notification", Map.of("hotelId", hotelId, "data", bookingData));
                log.info("Đã gửi thông báo booking mới tới room {} và super_admin_accounts", roomName);
            } catch (RuntimeException exception) {
                log.error("Không thể gửi thông báo booking mới {} tới chi nhánh {}", booking.getId(), hotelId, exception);
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

    public void emitPaymentReceived(Order order, BigDecimal amount, PaymentType paymentType) {
        if (order == null || order.getBooking() == null) return;

        Booking booking = order.getBooking();
        Long hotelId = booking.getHotel() != null ? booking.getHotel().getId() : null;
        if (hotelId == null) return;

        Customer customer = booking.getCustomer();
        String customerName = customer != null && customer.getFullName() != null
                ? customer.getFullName()
                : "Chưa cập nhật";
        String customerPhone = customer != null && customer.getPhone() != null
                ? customer.getPhone()
                : "Chưa cập nhật";
        String formattedAmount = formatAmount(amount != null ? amount : BigDecimal.ZERO);
        String paymentMethod = switch (paymentType != null ? paymentType : PaymentType.CASH) {
            case CASH -> "Tiền mặt";
            case BANK -> "Chuyển khoản (VietQR)";
            case EWALLET -> "Ví điện tử";
        };

        boolean isOnline = booking.getBookingChannel() == iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel.ONLINE;
        String title = isOnline ? "Đặt phòng & Thanh toán thành công" : "Đã nhận thanh toán booking";
        String description = "Mã booking: #" + booking.getId()
                + " | Khách hàng: " + customerName
                + " | SĐT: " + customerPhone
                + " | Số tiền thanh toán: " + formattedAmount + "đ"
                + " | Phương thức: " + paymentMethod + ".";

        notificationService.createAndPublishInCurrentTransaction(
                hotelId,
                NotificationType.PAYMENT_RECEIVED,
                title,
                description,
                "BOOKING",
                booking.getId()
        );

        Map<String, Object> paymentData = Map.of(
                "action", "PAYMENT_RECEIVED",
                "bookingId", booking.getId(),
                "customerName", customerName,
                "customerPhone", customerPhone,
                "amount", (amount != null ? amount : BigDecimal.ZERO).doubleValue(),
                "paymentMethod", paymentMethod
        );
        socketIOServer.getRoomOperations("hotel_" + hotelId)
                .sendEvent("new_booking_notification", paymentData);
        socketIOServer.getRoomOperations("super_admin_accounts")
                .sendEvent("new_booking_notification", Map.of("hotelId", hotelId, "data", paymentData));

        if (customer != null) {
            emitCustomerBookingStatus(
                    customer,
                    booking,
                    NotificationType.PAYMENT_RECEIVED,
                    "Đặt phòng & Thanh toán thành công",
                    "Mã booking: #" + booking.getId()
                            + " | Quý khách đã thanh toán thành công " + formattedAmount + "đ qua " + paymentMethod + ".");
        }

        emitRoomMatrixUpdate(hotelId);
    }

    public void emitBookingUpdated(
            Long hotelId,
            Booking booking,
            BookingModificationRequest request,
            BigDecimal oldTotalAmount,
            BigDecimal newTotalAmount) {
        if (hotelId == null || booking == null) return;

        Customer customer = booking.getCustomer();
        String customerName = customer != null && customer.getFullName() != null
                ? customer.getFullName()
                : "Chưa cập nhật";
        String customerPhone = customer != null && customer.getPhone() != null
                ? customer.getPhone()
                : "Chưa cập nhật";
        String amountChange = newTotalAmount.subtract(oldTotalAmount).signum() > 0 ? "+" : "";
        String description = "Mã booking: #" + booking.getId()
                + " | Khách hàng: " + customerName
                + " | SĐT: " + customerPhone
                + " | Nội dung cập nhật: " + describeBookingChanges(request)
                + " | Tổng tiền: " + formatAmount(oldTotalAmount) + "đ → " + formatAmount(newTotalAmount) + "đ"
                + " | Chênh lệch: " + amountChange + formatAmount(newTotalAmount.subtract(oldTotalAmount)) + "đ.";

        notificationService.createAndPublish(
                hotelId,
                NotificationType.BOOKING_UPDATED,
                "Booking đã được chỉnh sửa",
                description,
                "BOOKING",
                booking.getId()
        );

        Map<String, Object> bookingData = Map.of(
                "action", "UPDATED",
                "bookingId", booking.getId(),
                "customerName", customerName,
                "customerPhone", customerPhone,
                "totalAmount", newTotalAmount.doubleValue(),
                "description", description
        );
        socketIOServer.getRoomOperations("hotel_" + hotelId)
                .sendEvent("new_booking_notification", bookingData);
        socketIOServer.getRoomOperations("super_admin_accounts")
                .sendEvent("new_booking_notification", Map.of("hotelId", hotelId, "data", bookingData));
        emitRoomMatrixUpdate(hotelId);
    }

    private String describeBookingChanges(BookingModificationRequest request) {
        List<String> changes = new java.util.ArrayList<>();
        if (request.getBookingDetailIdsToCancel() != null && !request.getBookingDetailIdsToCancel().isEmpty()) {
            changes.add("hủy " + request.getBookingDetailIdsToCancel().size() + " phòng");
        }
        if (request.getRoomsToAdd() != null && !request.getRoomsToAdd().isEmpty()) {
            changes.add("thêm " + request.getRoomsToAdd().size() + " phòng");
        }
        if (request.getRoomsToChange() != null && !request.getRoomsToChange().isEmpty()) {
            changes.add("điều chỉnh " + request.getRoomsToChange().size() + " phòng/khung ngày/số khách");
        }
        if (request.getServicesToAddForExistingRooms() != null && !request.getServicesToAddForExistingRooms().isEmpty()) {
            int count = request.getServicesToAddForExistingRooms().stream()
                    .mapToInt(item -> item.getServices() == null ? 0 : item.getServices().size())
                    .sum();
            changes.add("thêm " + count + " dịch vụ");
        }
        if (request.getServiceQuantityUpdates() != null && !request.getServiceQuantityUpdates().isEmpty()) {
            changes.add("cập nhật số lượng dịch vụ cho " + request.getServiceQuantityUpdates().size() + " phòng");
        }
        if (request.getServicesToCancel() != null && !request.getServicesToCancel().isEmpty()) {
            int count = request.getServicesToCancel().stream()
                    .mapToInt(item -> item.getServiceDetailIds() == null ? 0 : item.getServiceDetailIds().size())
                    .sum();
            changes.add("hủy " + count + " dịch vụ");
        }
        if (Boolean.TRUE.equals(request.getRemovePromotion())) {
            changes.add("gỡ khuyến mãi");
        } else if (request.getPromotionRequest() != null || request.getCustomerPromotionRequest() != null) {
            changes.add("cập nhật khuyến mãi");
        }
        return changes.isEmpty() ? "cập nhật thông tin booking" : String.join(", ", changes);
    }

    private String formatAmount(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount);
    }

    // chua co function cho huy booking
    public void emitBookingCancelled(Long hotelId, String bookingId, String reason) {
        if (hotelId == null) return;
        notificationService.createAndPublish(
                hotelId,
                NotificationType.BOOKING_CANCELLED,
                "Booking đã bị hủy",
                "Booking #" + bookingId + " đã bị hủy" + (reason == null || reason.isBlank() ? "." : ": " + reason),
                "BOOKING",
                bookingId);
    }

    // đã giải quyết xong luồng hoàn tiền do hủy booking
    public void emitRefundCompleted(Long hotelId, String bookingId, BigDecimal refundAmount) {
        if (hotelId == null) return;
        String formattedAmount = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                .format(refundAmount);
        notificationService.createAndPublish(
                hotelId,
                NotificationType.BOOKING_REFUND_COMPLETED,
                "Đã hoàn tiền",
                "Đã xác nhận hoàn " + formattedAmount + "đ cho booking #" + bookingId + ".",
                "BOOKING_CANCELLATION",
                bookingId);
    }

    // thong bao huy booking
    public void emitRefundRequested(
            Long hotelId,
            String bookingId,
            BigDecimal refundAmount,
            BookingCancellationStatus status) {
        if (hotelId == null) return;
        String formattedAmount = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                .format(refundAmount);
        notificationService.createAndPublish(
                hotelId,
                NotificationType.BOOKING_REFUND_REQUESTED,
                "Yêu cầu hoàn tiền mới",
                "Booking #" + bookingId + " có yêu cầu hoàn " + formattedAmount
                        + "đ (trạng thái: " + status + "). Dự kiến xử lý trong vòng 1–3 ngày.",
                "BOOKING_CANCELLATION",
                bookingId);
    }


    /**
     * 3. Gửi thông báo về cho chính Khách hàng (trạng thái đơn, thanh toán...)
     */
    public void emitCustomerBookingStatus(
            Customer customer,
            Booking booking,
            NotificationType notificationType,
            String title,
            String description
    ) {
        if (customer != null && booking != null) {
            String roomName = "user_" + customer.getId();
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("customer_booking_updated", booking);
            log.info("👤 [Booking Socket] Đã gửi trạng thái tới user room: {}", roomName);

            if (customer.getAccount() != null && customer.getAccount().getId() != null) {
                Long hotelId = booking.getHotel() == null ? null : booking.getHotel().getId();
                notificationService.createAndPublishToCustomer(
                        customer.getAccount().getId(),
                        hotelId,
                        notificationType,
                        title,
                        description,
                        "BOOKING",
                        booking.getId());
            }
        }
    }
    //

    public void emitLateCheckOutAlert(Long hotelId, LateCheckOutBookingNotificationResponse bookingData) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("late_check_out_calendar", bookingData);

            log.info("[Checkout late!!!!!]");
        }
    }

    }

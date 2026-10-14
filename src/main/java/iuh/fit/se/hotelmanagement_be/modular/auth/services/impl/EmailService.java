package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String email, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("OTP has been sent");
        message.setText("Ma OTP cua ban: " + otpCode);
        mailSender.send(message);

    }

    public void sendBookingCancellationRequestedEmail(
            String email,
            String customerName,
            String bookingId,
            String hotelName,
            String bookingChannel,
            String bookingDetails,
            String refundAmount,
            boolean refundExpected) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Xác nhận yêu cầu hủy booking " + bookingId);
        String greeting = customerName == null || customerName.isBlank()
                ? "Quý khách"
                : customerName.trim();
        StringBuilder body = new StringBuilder()
                .append("Kính gửi ").append(greeting).append(",\n\n")
                .append("Khách sạn đã tiếp nhận yêu cầu hủy booking ").append(bookingId).append(".\n")
                .append("Khách sạn: ").append(hotelName).append("\n")
                .append("Loại booking: ").append(bookingChannel).append("\n")
                .append("Chi tiết đặt phòng:\n").append(bookingDetails).append("\n");
        if (refundExpected) {
            body.append("Số tiền dự kiến hoàn: ").append(refundAmount).append("\n")
                    .append("Yêu cầu hoàn tiền sẽ được xử lý trong vòng 1–3 ngày. ")
                    .append("Khách sạn sẽ gửi email thông báo khi đã hoàn tiền.\n");
        } else {
            body.append("Yêu cầu hủy đã được ghi nhận. Booking này không phát sinh khoản hoàn tiền.\n");
        }
        body.append("\nTrân trọng,\n").append(hotelName);
        message.setText(body.toString());
        mailSender.send(message);
    }

    public void sendRefundCompletedEmail(
            String email,
            String customerName,
            String bookingId,
            String hotelName,
            String refundAmount,
            String refundReference) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Đã hoàn tiền booking " + bookingId);
        String greeting = customerName == null || customerName.isBlank()
                ? "Quý khách"
                : customerName.trim();
        StringBuilder body = new StringBuilder()
                .append("Kính gửi ").append(greeting).append(",\n\n")
                .append("Khách sạn đã hoàn tiền cho booking ").append(bookingId).append(".\n")
                .append("Khách sạn: ").append(hotelName).append("\n")
                .append("Số tiền đã hoàn: ").append(refundAmount).append("\n");
        if (refundReference != null && !refundReference.isBlank()) {
            body.append("Mã giao dịch / ghi chú: ").append(refundReference.trim()).append("\n");
        }
        body.append("\nTrân trọng,\n").append(hotelName);
        message.setText(body.toString());
        mailSender.send(message);
    }

    public String getCustomerEmail(Customer customer) {
        if (customer == null) {
            return null;
        }
        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            return customer.getEmail().trim();
        }
        if (customer.getAccount() != null
                && customer.getAccount().getEmail() != null
                && !customer.getAccount().getEmail().isBlank()) {
            return customer.getAccount().getEmail().trim();
        }
        return null;
    }

    public void sendBookingCreatedEmail(
            String email,
            String customerName,
            String bookingId,
            String hotelName,
            String bookingDetails,
            BigDecimal totalAmount) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Xác nhận đặt phòng " + bookingId);
        message.setText(new StringBuilder()
                .append("Kính gửi ").append(greeting(customerName)).append(",\n\n")
                .append("Booking của quý khách đã được tạo thành công.\n")
                .append("Mã booking: ").append(bookingId).append("\n")
                .append("Khách sạn: ").append(hotelName).append("\n")
                .append("Chi tiết đặt phòng:\n").append(bookingDetails).append("\n")
                .append("Tổng tiền tạm tính: ").append(formatAmount(totalAmount)).append("\n\n")
                .append("Trân trọng,\n").append(hotelName)
                .toString());
        mailSender.send(message);
    }

    public void sendPaymentReceivedEmail(
            String email,
            String customerName,
            String bookingId,
            String orderId,
            String hotelName,
            BigDecimal receivedAmount,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            String paymentType,
            String paymentReference,
            LocalDateTime paidAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Xác nhận thanh toán booking " + bookingId);
        StringBuilder body = new StringBuilder()
                .append("Kính gửi ").append(greeting(customerName)).append(",\n\n")
                .append("Khách sạn đã ghi nhận thanh toán cho booking ").append(bookingId).append(".\n")
                .append("Khách sạn: ").append(hotelName).append("\n")
                .append("Mã hóa đơn: ").append(orderId).append("\n")
                .append("Số tiền vừa thanh toán: ").append(formatAmount(receivedAmount)).append("\n")
                .append("Tổng đã thanh toán: ").append(formatAmount(paidAmount)).append("\n")
                .append("Số tiền còn lại: ").append(formatAmount(remainingAmount)).append("\n")
                .append("Phương thức: ").append(paymentType).append("\n");
        if (paidAt != null) {
            body.append("Thời gian: ").append(paidAt).append("\n");
        }
        if (paymentReference != null && !paymentReference.isBlank()) {
            body.append("Mã tham chiếu: ").append(paymentReference).append("\n");
        }
        body.append("\nTrân trọng,\n").append(hotelName);
        message.setText(body.toString());
        mailSender.send(message);
    }

    public void sendCheckoutInvoiceEmail(
            String email,
            String customerName,
            String bookingId,
            String hotelName,
            String invoiceDetails) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Hóa đơn checkout booking " + bookingId);
        message.setText(new StringBuilder()
                .append("Kính gửi ").append(greeting(customerName)).append(",\n\n")
                .append("Booking ").append(bookingId).append(" đã checkout hoàn tất.\n")
                .append("Khách sạn: ").append(hotelName).append("\n\n")
                .append("HÓA ĐƠN THANH TOÁN\n")
                .append(invoiceDetails).append("\n\n")
                .append("Trân trọng,\n").append(hotelName)
                .toString());
        mailSender.send(message);
    }

    public void sendAfterCommit(String reference, Runnable emailAction) {
        Runnable safeEmailAction = () -> {
            try {
                emailAction.run();
            } catch (RuntimeException exception) {
                log.error("Không gửi được email cho {}: {}", reference, exception.getMessage(), exception);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeEmailAction.run();
                }
            });
        } else {
            safeEmailAction.run();
        }
    }

    private String greeting(String customerName) {
        return customerName == null || customerName.isBlank() ? "Quý khách" : customerName.trim();
    }

    private String formatAmount(BigDecimal amount) {
        return (amount != null ? amount : BigDecimal.ZERO).toPlainString() + " VND";
    }
}

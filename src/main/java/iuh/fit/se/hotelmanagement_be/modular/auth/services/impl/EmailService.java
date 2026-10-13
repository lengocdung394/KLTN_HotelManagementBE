package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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

}

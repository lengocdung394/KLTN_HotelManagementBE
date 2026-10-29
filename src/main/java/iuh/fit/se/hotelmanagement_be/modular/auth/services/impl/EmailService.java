package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class EmailService {
    public record BookingServiceEmailDetail(String name, int quantity, BigDecimal unitPrice) {}

    public record BookingRoomEmailDetail(
            String roomNumber,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            List<BookingServiceEmailDetail> services) {}

    private static final DateTimeFormatter BOOKING_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String email, String otpCode) {
        String subject = "Mã xác thực tài khoản";
        String plain = "Mã xác thực của bạn: " + otpCode;
        String content = "<p style=\"margin:0 0 18px;color:#475569;font-size:15px;line-height:1.7\">"
                + "Sử dụng mã bên dưới để tiếp tục. Vui lòng không chia sẻ mã này với người khác.</p>"
                + "<div style=\"padding:18px;text-align:center;border-radius:12px;background:#eff6ff;"
                + "color:#1d4ed8;font-size:30px;font-weight:800;letter-spacing:8px\">"
                + escapeHtml(otpCode) + "</div>";
        sendBrandedEmail(email, subject, "BẢO MẬT TÀI KHOẢN", "Mã xác thực của bạn", plain,
                "Sen Việt", content);
    }

    public void sendBookingCancellationRequestedEmail(
            String email,
            String customerName,
            String bookingId,
            String hotelName,
            String bookingChannel,
            String bookingDetails,
            String refundAmount,
            boolean refundExpected,
            boolean cashRefundCompleted) {
        String subject = "Xác nhận yêu cầu hủy booking " + bookingId;
        String greeting = greeting(customerName);
        StringBuilder body = new StringBuilder()
                .append("Kính gửi ").append(greeting).append(",\n\n")
                .append("Khách sạn đã tiếp nhận yêu cầu hủy booking ").append(bookingId).append(".\n")
                .append("Khách sạn: ").append(hotelName).append("\n")
                .append("Loại booking: ").append(bookingChannel).append("\n")
                .append("Chi tiết đặt phòng:\n").append(bookingDetails).append("\n");
        if (refundExpected) {
            if (cashRefundCompleted) {
                body.append("Số tiền đã hoàn trực tiếp bằng tiền mặt: ").append(refundAmount).append("\n");
            } else {
                body.append("Số tiền dự kiến hoàn: ").append(refundAmount).append("\n")
                        .append("Yêu cầu hoàn tiền sẽ được xử lý trong vòng 1–3 ngày. ")
                        .append("Khách sạn sẽ gửi email thông báo khi đã hoàn tiền.\n");
            }
        } else {
            body.append("Yêu cầu hủy đã được ghi nhận. Booking này không phát sinh khoản hoàn tiền.\n");
        }
        body.append("\nTrân trọng,\n").append(hotelName);
        StringBuilder content = new StringBuilder()
                .append(paragraph("Kính gửi " + greeting + ", khách sạn đã tiếp nhận yêu cầu hủy đặt phòng của quý khách."))
                .append(detailRow("Mã booking", bookingId, true))
                .append(detailRow("Khách sạn", hotelName, false))
                .append(detailRow("Loại booking", bookingChannel, false))
                .append(section("Chi tiết đặt phòng", htmlLines(bookingDetails)));
        if (refundExpected && cashRefundCompleted) {
            content.append(notice("Đã hoàn tiền", "Số tiền đã hoàn trực tiếp bằng tiền mặt: " + refundAmount, "#ecfdf5", "#047857"));
        } else if (refundExpected) {
            content.append(notice("Đang xử lý hoàn tiền",
                    "Số tiền dự kiến hoàn: " + refundAmount
                            + ". Yêu cầu hoàn tiền sẽ được xử lý trong vòng 1–3 ngày. Khách sạn sẽ gửi email khi hoàn tất.",
                    "#fffbeb", "#b45309"));
        } else {
            content.append(notice("Đã ghi nhận yêu cầu hủy",
                    "Booking này không phát sinh khoản hoàn tiền.", "#f1f5f9", "#475569"));
        }
        sendBrandedEmail(email, subject, "CẬP NHẬT ĐẶT PHÒNG", "Yêu cầu hủy đã được tiếp nhận",
                body.toString(), hotelName, content.toString());
    }

    public void sendRefundCompletedEmail(
            String email,
            String customerName,
            String bookingId,
            String hotelName,
            String refundAmount,
            String refundReference) {
        String subject = "Đã hoàn tiền booking " + bookingId;
        String greeting = greeting(customerName);
        StringBuilder body = new StringBuilder()
                .append("Kính gửi ").append(greeting).append(",\n\n")
                .append("Khách sạn đã hoàn tiền cho booking ").append(bookingId).append(".\n")
                .append("Khách sạn: ").append(hotelName).append("\n")
                .append("Số tiền đã hoàn: ").append(refundAmount).append("\n");
        StringBuilder content = new StringBuilder()
                .append(paragraph("Kính gửi " + greeting + ", khoản hoàn tiền cho booking của quý khách đã được thực hiện."))
                .append(detailRow("Mã booking", bookingId, false))
                .append(detailRow("Khách sạn", hotelName, false))
                .append(detailRow("Số tiền đã hoàn", refundAmount, true));
        if (refundReference != null && !refundReference.isBlank()) {
            body.append("Mã giao dịch / ghi chú: ").append(refundReference.trim()).append("\n");
            content.append(detailRow("Mã giao dịch / ghi chú", refundReference.trim(), false));
        }
        body.append("\nTrân trọng,\n").append(hotelName);
        sendBrandedEmail(email, subject, "HOÀN TIỀN THÀNH CÔNG", "Khoản hoàn tiền đã được thực hiện",
                body.toString(), hotelName, content.toString());
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

    @Async("emailTaskExecutor")
    public void sendBookingCreatedEmail(
            String email,
            String customerName,
            String bookingId,
            String hotelName,
            List<BookingRoomEmailDetail> bookingDetails,
            BigDecimal totalAmount,
            String promotionName,
            String promotionCode,
            BigDecimal promotionDiscount,
            boolean paymentDueWithinFiveMinutes) {
        String subject = "Xác nhận đặt phòng " + bookingId;
        String plainBookingDetails = bookingDetails.stream()
                .map(detail -> "Phòng " + valueOrDash(detail.roomNumber())
                        + " | Check-in: " + formatBookingDateTime(detail.checkIn())
                        + " | Check-out: " + formatBookingDateTime(detail.checkOut())
                        + plainServices(detail.services()))
                .reduce((first, next) -> first + "\n" + next)
                .orElse("Chưa có thông tin phòng");
        StringBuilder plain = new StringBuilder()
                .append("Kính gửi ").append(greeting(customerName)).append(",\n\n")
                .append("Booking của quý khách đã được tạo thành công.\n")
                .append("Mã booking: ").append(bookingId).append("\n")
                .append("Khách sạn: ").append(hotelName).append("\n")
                .append("Chi tiết đặt phòng:\n").append(plainBookingDetails).append("\n")
                .append("Tổng tiền tạm tính: ").append(formatAmount(totalAmount)).append("\n");
        if (promotionCode != null && !promotionCode.isBlank()) {
            plain.append("Khuyến mãi: ").append(valueOrDash(promotionName))
                    .append(" (").append(promotionCode).append(")\n")
                    .append("Số tiền được giảm: ").append(formatAmount(promotionDiscount)).append("\n");
        }
        if (paymentDueWithinFiveMinutes) {
            plain.append("\nLưu ý: Vui lòng thanh toán trong vòng 5 phút kể từ thời điểm tạo booking để giữ phòng. "
                    + "Sau thời gian này, booking có thể hết hạn và phòng được mở lại.\n");
        }
        plain.append("\nTrân trọng,\n").append(hotelName);
        String content = paragraph("Kính gửi " + greeting(customerName) + ", đặt phòng của quý khách đã được tạo thành công.")
                + detailRow("Mã booking", bookingId, true)
                + detailRow("Khách sạn", hotelName, false)
                + bookingRoomDetailsTable(bookingDetails)
                + bookingServicesTable(bookingDetails)
                + detailRow("Tổng tiền tạm tính", formatAmount(totalAmount), true);
        if (promotionCode != null && !promotionCode.isBlank()) {
            content += detailRow("Khuyến mãi", valueOrDash(promotionName), false)
                    + detailRow("Mã khuyến mãi", promotionCode, false)
                    + detailRow("Số tiền được giảm", formatAmount(promotionDiscount), true);
        }
        if (paymentDueWithinFiveMinutes) {
            content += notice(
                    "Lưu ý để giữ phòng",
                    "Vui lòng thanh toán trong vòng 5 phút kể từ thời điểm tạo booking. Sau thời gian này, booking có thể hết hạn và phòng được mở lại.",
                    "#fff7ed",
                    "#9a3412");
        }
        try {
            sendBrandedEmail(email, subject, "XÁC NHẬN ĐẶT PHÒNG", "Đặt phòng đã được ghi nhận",
                    plain.toString(), hotelName, content);
            log.info("SMTP đã chấp nhận email xác nhận booking {} tới {}", bookingId, maskEmail(email));
        } catch (RuntimeException exception) {
            log.error("Không gửi được email xác nhận booking {}: {}", bookingId, exception.getMessage(), exception);
        }
    }

    @Async("emailTaskExecutor")
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
        String subject = "Xác nhận thanh toán booking " + bookingId;
        StringBuilder body = new StringBuilder()
                .append("Kính gửi ").append(greeting(customerName)).append(",\n\n")
                .append("Khách sạn đã ghi nhận thanh toán cho booking ").append(bookingId).append(".\n")
                .append("Khách sạn: ").append(hotelName).append("\n")
                .append("Mã hóa đơn: ").append(orderId).append("\n")
                .append("Số tiền vừa thanh toán: ").append(formatAmount(receivedAmount)).append("\n")
                .append("Tổng đã thanh toán: ").append(formatAmount(paidAmount)).append("\n")
                .append("Số tiền còn lại: ").append(formatAmount(remainingAmount)).append("\n")
                .append("Phương thức: ").append(paymentType).append("\n");
        StringBuilder content = new StringBuilder()
                .append(paragraph("Kính gửi " + greeting(customerName) + ", khách sạn đã ghi nhận khoản thanh toán của quý khách."))
                .append(detailRow("Mã booking", bookingId, false))
                .append(detailRow("Mã hóa đơn", orderId, false))
                .append(detailRow("Số tiền vừa thanh toán", formatAmount(receivedAmount), true))
                .append(detailRow("Tổng đã thanh toán", formatAmount(paidAmount), false))
                .append(detailRow("Số tiền còn lại", formatAmount(remainingAmount), false))
                .append(detailRow("Phương thức", paymentType, false));
        if (paidAt != null) {
            body.append("Thời gian: ").append(paidAt).append("\n");
            content.append(detailRow("Thời gian", paidAt.toString(), false));
        }
        body.append("\nTrân trọng,\n").append(hotelName);
        try {
            sendBrandedEmail(email, subject, "XÁC NHẬN THANH TOÁN", "Thanh toán đã được ghi nhận",
                    body.toString(), hotelName, content.toString());
        } catch (RuntimeException exception) {
            log.error("Không gửi được email xác nhận thanh toán cho booking {}: {}", bookingId, exception.getMessage(), exception);
        }
    }

    public void sendCheckoutInvoiceEmail(
            String email,
            String customerName,
            String bookingId,
            String hotelName,
            String invoiceDetails) {
        String subject = "Hóa đơn checkout booking " + bookingId;
        String plain = new StringBuilder()
                .append("Kính gửi ").append(greeting(customerName)).append(",\n\n")
                .append("Booking ").append(bookingId).append(" đã checkout hoàn tất.\n")
                .append("Khách sạn: ").append(hotelName).append("\n\n")
                .append("HÓA ĐƠN THANH TOÁN\n")
                .append(invoiceDetails).append("\n\n")
                .append("Trân trọng,\n").append(hotelName)
                .toString();
        String content = paragraph("Kính gửi " + greeting(customerName) + ", booking " + bookingId + " đã checkout hoàn tất.")
                + detailRow("Khách sạn", hotelName, false)
                + section("Hóa đơn thanh toán", htmlLines(invoiceDetails));
        sendBrandedEmail(email, subject, "HÓA ĐƠN CHECKOUT", "Cảm ơn quý khách đã lưu trú",
                plain, hotelName, content);
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

    private String bookingRoomDetailsTable(List<BookingRoomEmailDetail> bookingDetails) {
        StringBuilder table = new StringBuilder()
                .append("<div style=\"margin-top:20px;padding:16px;border:1px solid #e2e8f0;border-radius:10px;background:#f8fafc\">")
                .append("<div style=\"margin-bottom:10px;color:#334155;font-size:13px;font-weight:700\">Chi tiết đặt phòng</div>")
                .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"width:100%;table-layout:fixed;border-collapse:collapse\">")
                .append("<thead><tr>")
                .append(bookingTableHeader("Số phòng", "24%"))
                .append(bookingTableHeader("Check-in", "38%"))
                .append(bookingTableHeader("Check-out", "38%"))
                .append("</tr></thead><tbody>");
        if (bookingDetails.isEmpty()) {
            table.append("<tr><td colspan=\"3\" style=\"padding:12px 4px;color:#64748b;font-size:12px\">Chưa có thông tin phòng</td></tr>");
        } else {
            for (BookingRoomEmailDetail detail : bookingDetails) {
                table.append("<tr>")
                        .append(bookingTableCell(valueOrDash(detail.roomNumber())))
                        .append(bookingTableCell(formatBookingDateTime(detail.checkIn())))
                        .append(bookingTableCell(formatBookingDateTime(detail.checkOut())))
                        .append("</tr>");
            }
        }
        return table.append("</tbody></table></div>").toString();
    }

    private String bookingServicesTable(List<BookingRoomEmailDetail> bookingDetails) {
        List<BookingRoomEmailDetail> roomsWithServices = bookingDetails.stream()
                .filter(detail -> detail.services() != null && !detail.services().isEmpty())
                .toList();
        if (roomsWithServices.isEmpty()) {
            return "";
        }

        StringBuilder table = new StringBuilder()
                .append("<div style=\"margin-top:16px;padding:16px;border:1px solid #e2e8f0;border-radius:10px;background:#ffffff\">")
                .append("<div style=\"margin-bottom:10px;color:#334155;font-size:13px;font-weight:700\">Dịch vụ đã đặt</div>")
                .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"width:100%;table-layout:fixed;border-collapse:collapse\">")
                .append("<thead><tr>")
                .append(bookingTableHeader("Phòng", "20%"))
                .append(bookingTableHeader("Dịch vụ", "38%"))
                .append(bookingTableHeader("SL", "12%"))
                .append(bookingTableHeader("Thành tiền", "30%"))
                .append("</tr></thead><tbody>");
        for (BookingRoomEmailDetail room : roomsWithServices) {
            for (BookingServiceEmailDetail service : room.services()) {
                BigDecimal lineTotal = serviceLineTotal(service);
                table.append("<tr>")
                        .append(bookingTableCell(valueOrDash(room.roomNumber())))
                        .append(bookingTableCell(valueOrDash(service.name())))
                        .append(bookingTableCell(String.valueOf(service.quantity())))
                        .append(bookingTableCell(formatAmount(lineTotal)))
                        .append("</tr>");
            }
        }
        return table.append("</tbody></table></div>").toString();
    }

    private String plainServices(List<BookingServiceEmailDetail> services) {
        if (services == null || services.isEmpty()) {
            return "";
        }
        return services.stream()
                .map(service -> "\n  - " + valueOrDash(service.name())
                        + " x" + service.quantity()
                        + " (" + formatAmount(serviceLineTotal(service)) + ")")
                .reduce("", String::concat);
    }

    private BigDecimal serviceLineTotal(BookingServiceEmailDetail service) {
        BigDecimal unitPrice = service.unitPrice() != null ? service.unitPrice() : BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(service.quantity()));
    }

    private String bookingTableHeader(String label, String width) {
        return "<th align=\"left\" width=\"" + width + "\" style=\"padding:8px 4px;border-bottom:1px solid #cbd5e1;color:#64748b;font-size:11px;font-weight:700\">"
                + escapeHtml(label) + "</th>";
    }

    private String bookingTableCell(String value) {
        return "<td style=\"padding:10px 4px;border-bottom:1px solid #e2e8f0;color:#334155;font-size:12px;line-height:1.5;vertical-align:top;word-break:break-word\">"
                + escapeHtml(value) + "</td>";
    }

    private String formatBookingDateTime(LocalDateTime value) {
        return value == null ? "—" : value.format(BOOKING_DATE_TIME_FORMAT);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    private String maskEmail(String email) {
        int atIndex = email == null ? -1 : email.indexOf('@');
        if (atIndex <= 0) {
            return "[email không hợp lệ]";
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String formatAmount(BigDecimal amount) {
        return (amount != null ? amount : BigDecimal.ZERO).toPlainString() + " VND";
    }

    private void sendBrandedEmail(
            String email,
            String subject,
            String eyebrow,
            String title,
            String plainText,
            String hotelName,
            String content) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(plainText, emailTemplate(hotelName, subject, eyebrow, title, content));
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Không thể tạo email HTML: " + subject, exception);
        }
    }

    private String emailTemplate(String hotelName, String preheader, String eyebrow, String title, String content) {
        String brand = hotelName == null || hotelName.isBlank() ? "Sen Việt" : hotelName.trim();
        return "<!doctype html><html lang=\"vi\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + escapeHtml(preheader) + "</title></head>"
                + "<body style=\"margin:0;padding:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;color:#0f172a\">"
                + "<div style=\"display:none;max-height:0;overflow:hidden;opacity:0\">" + escapeHtml(preheader) + "</div>"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:#f1f5f9;padding:28px 12px\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"600\" cellspacing=\"0\" cellpadding=\"0\" style=\"width:100%;max-width:600px;background:#ffffff;border-radius:16px;overflow:hidden\">"
                + "<tr><td style=\"padding:24px 30px;background:#172554;color:#ffffff\"><div style=\"font-size:12px;letter-spacing:1.5px;text-transform:uppercase;color:#bfdbfe\">"
                + escapeHtml(eyebrow) + "</div><div style=\"margin-top:7px;font-size:20px;font-weight:700\">" + escapeHtml(brand) + "</div></td></tr>"
                + "<tr><td style=\"padding:30px\">"
                + "<h1 style=\"margin:0 0 22px;color:#0f172a;font-size:22px;line-height:1.35\">" + escapeHtml(title) + "</h1>"
                + content
                + "<div style=\"margin-top:28px;padding-top:18px;border-top:1px solid #e2e8f0;color:#64748b;font-size:13px;line-height:1.6\">"
                + "Trân trọng,<br><strong style=\"color:#334155\">" + escapeHtml(brand) + "</strong></div>"
                + "</td></tr><tr><td style=\"padding:16px 30px;background:#f8fafc;color:#94a3b8;font-size:11px;line-height:1.5;text-align:center\">"
                + "Email được gửi tự động, vui lòng không trả lời trực tiếp thư này.</td></tr></table>"
                + "</td></tr></table></body></html>";
    }

    private String paragraph(String text) {
        return "<p style=\"margin:0 0 20px;color:#475569;font-size:14px;line-height:1.7\">" + escapeHtml(text) + "</p>";
    }

    private String detailRow(String label, String value, boolean emphasized) {
        String color = emphasized ? "#1d4ed8" : "#334155";
        String weight = emphasized ? "700" : "500";
        return "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-bottom:1px solid #e2e8f0\"><tr>"
                + "<td style=\"padding:12px 0;color:#64748b;font-size:13px\">" + escapeHtml(label) + "</td>"
                + "<td align=\"right\" style=\"padding:12px 0;color:" + color + ";font-size:13px;font-weight:" + weight + "\">"
                + escapeHtml(value) + "</td></tr></table>";
    }

    private String section(String heading, String content) {
        return "<div style=\"margin-top:20px;padding:16px;border:1px solid #e2e8f0;border-radius:10px;background:#f8fafc\">"
                + "<div style=\"margin-bottom:10px;color:#334155;font-size:13px;font-weight:700\">" + escapeHtml(heading) + "</div>"
                + "<div style=\"color:#475569;font-size:13px;line-height:1.8\">" + content + "</div></div>";
    }

    private String notice(String heading, String message, String background, String color) {
        return "<div style=\"margin-top:20px;padding:15px 16px;border-radius:10px;background:" + background + ";color:" + color + "\">"
                + "<div style=\"font-size:13px;font-weight:700\">" + escapeHtml(heading) + "</div>"
                + "<div style=\"margin-top:5px;font-size:13px;line-height:1.6\">" + escapeHtml(message) + "</div></div>";
    }

    private String htmlLines(String text) {
        return escapeHtml(text).replace("\r\n", "<br>").replace("\n", "<br>");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "—";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

package iuh.fit.se.hotelmanagement_be.config;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import iuh.fit.se.hotelmanagement_be.modular.chat.services.LiveChatService;

@Configuration
@Slf4j
public class SocketIOConfig {
    private final AccountRepository accountRepository;
    private final LiveChatService liveChatService;

    public SocketIOConfig(AccountRepository accountRepository, LiveChatService liveChatService) {
        this.accountRepository = accountRepository;
        this.liveChatService = liveChatService;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250655368566D5971";
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec originalKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(originalKey).build();
    }


    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();

        config.setHostname("0.0.0.0");
        config.setPort(8085);
        config.setOrigin("*");
        config.setJsonSupport(new com.corundumstudio.socketio.protocol.JacksonJsonSupport(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()));

        config.setAuthorizationListener(handshakeData -> {
            String token = handshakeData.getSingleUrlParam("token");

            if (token != null && !token.isEmpty() && !"null".equalsIgnoreCase(token) && !"undefined".equalsIgnoreCase(token)) {
                try {
                    Jwt jwt = jwtDecoder().decode(token);
                    log.info("Xác thực thành công cho user: {}", jwt.getSubject());
                    return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;

                } catch (JwtException e) {
                    log.warn("Token không hợp lệ hoặc hết hạn: {} -> Tiếp tục kết nối với quyền khách/guest", e.getMessage());
                    return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
                }
            }
            log.info("Khách vãng lai (Guest) kết nối Socket.IO thành công");
            return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
        });

        SocketIOServer server = new SocketIOServer(config);

        server.addEventListener("join_user_room", String.class, (client, userId, ackSender) -> {
            if (userId != null && !userId.trim().isEmpty()) {
                client.joinRoom(userId);
                client.joinRoom("user_" + userId);
                log.info("User {} joined their personal socket room ({} and user_{})", userId, userId, userId);
            } else {
                log.warn("join_user_room received with empty userId");
            }
        });
        server.addEventListener("join_hotel_room", String.class, (client, hotelId, ackSender) -> {
            if (hotelId != null && !hotelId.trim().isEmpty()) {
                client.joinRoom("hotel_" + hotelId);
                log.info("Client joined hotel room: hotel_{}", hotelId);
            } else {
                log.warn("join_hotel_room received with empty hotelId");
            }
        });

        server.addEventListener("join_super_admin_accounts_room", String.class, (client, ignored, ackSender) -> {
            String token = client.getHandshakeData().getSingleUrlParam("token");
            log.info("Received join_super_admin_accounts_room request from client {}", client.getSessionId());
            try {
                Jwt jwt = jwtDecoder().decode(token);
                boolean isSuperAdmin = jwt.getClaimAsStringList("roles") != null
                        && jwt.getClaimAsStringList("roles").stream()
                        .anyMatch(role -> "ROLE_SUPER_ADMIN".equals(role));
                if (isSuperAdmin) {
                    client.joinRoom("super_admin_accounts");
                    log.info("Super admin {} joined room super_admin_accounts", jwt.getSubject());
                } else {
                    log.warn("Denied access to super admin account socket room for subject {}. JWT roles: {}",
                            jwt.getSubject(), jwt.getClaimAsStringList("roles"));
                }
            } catch (JwtException e) {
                log.warn("Denied access to super admin account socket room: invalid token");
            } catch (RuntimeException e) {
                log.error("Could not add client {} to super_admin_accounts room", client.getSessionId(), e);
            }
        });

        // ==================== REAL-TIME CUSTOMER <-> STAFF CHAT ====================
        server.addEventListener("customer_send_staff_chat", java.util.Map.class, (client, data, ackSender) -> {
            log.info("📩 [SocketIO] Khách hàng gửi tin nhắn tới nhân viên: {}", data);

            Long hotelId = null;
            if (data.get("hotelId") != null) {
                try {
                    hotelId = Long.parseLong(data.get("hotelId").toString());
                } catch (NumberFormatException ignored) {}
            }

            String branchName = data.get("branchName") != null ? data.get("branchName").toString() : null;
            if (hotelId == null && branchName != null) {
                String lowerBranch = branchName.toLowerCase();
                if (lowerBranch.contains("sài gòn") || lowerBranch.contains("sai gon")) hotelId = 1L;
                else if (lowerBranch.contains("hà nội") || lowerBranch.contains("ha noi")) hotelId = 2L;
                else if (lowerBranch.contains("đà nẵng") || lowerBranch.contains("da nang")) hotelId = 3L;
                else if (lowerBranch.contains("nha trang")) hotelId = 4L;
                else if (lowerBranch.contains("đà lạt") || lowerBranch.contains("da lat")) hotelId = 5L;
                else if (lowerBranch.contains("gò công") || lowerBranch.contains("go cong")) hotelId = 6L;
                else if (lowerBranch.contains("an nhơn") || lowerBranch.contains("an nhon")) hotelId = 7L;
                else if (lowerBranch.contains("quy nhơn") || lowerBranch.contains("quy nhon")) hotelId = 8L;

                if (hotelId != null) {
                    data.put("hotelId", hotelId);
                }
            }

            try {
                String customerId = data.get("customerId") != null ? data.get("customerId").toString() : null;
                String customerName = data.get("customerName") != null ? data.get("customerName").toString() : null;
                String customerPhone = data.get("customerPhone") != null ? data.get("customerPhone").toString() : null;
                String message = data.get("message") != null ? data.get("message").toString() : null;
                String msgId = data.get("msgId") != null ? data.get("msgId").toString() : null;

                if (message != null && !message.trim().isEmpty()) {
                    liveChatService.saveCustomerMessage(
                            customerId, customerName, customerPhone, hotelId, branchName, message, msgId
                    );
                }
            } catch (Exception e) {
                log.error("Lỗi khi lưu tin nhắn khách hàng vào PostgreSQL: ", e);
            }

            // Kiểm tra số lượng nhân viên chi nhánh (hoặc Super Admin tổng đài) đang online trong room
            int staffClients = 0;
            if (hotelId != null) {
                staffClients = server.getRoomOperations("hotel_" + hotelId).getClients().size();
                server.getRoomOperations("hotel_" + hotelId).sendEvent("customer_staff_chat_message", data);
            } else {
                staffClients = server.getRoomOperations("super_admin_accounts").getClients().size();
                server.getRoomOperations("super_admin_accounts").sendEvent("customer_staff_chat_message", data);
            }
            boolean isBranchOnline = staffClients > 0;
            data.put("isBranchOnline", isBranchOnline);
            data.put("isRead", false);
            client.sendEvent("customer_staff_chat_ack", data);
        });

        // Kiểm tra trạng thái trực tuyến (online/offline) của chi nhánh hoặc Tổng đài Super Admin
        server.addEventListener("check_branch_online_status", java.util.Map.class, (client, data, ackSender) -> {
            Long hotelId = null;
            if (data != null && data.get("hotelId") != null) {
                try {
                    hotelId = Long.parseLong(data.get("hotelId").toString());
                } catch (NumberFormatException ignored) {}
            }
            int staffClients = (hotelId != null)
                    ? server.getRoomOperations("hotel_" + hotelId).getClients().size()
                    : server.getRoomOperations("super_admin_accounts").getClients().size();
            java.util.Map<String, Object> res = new java.util.HashMap<>();
            res.put("hotelId", hotelId);
            res.put("online", staffClients > 0);
            res.put("staffCount", staffClients);
            client.sendEvent("branch_online_status_result", res);
        });

        // Nhân viên đánh dấu đã đọc tin nhắn của khách hàng
        server.addEventListener("staff_mark_customer_read", java.util.Map.class, (client, data, ackSender) -> {
            log.info("👀 [SocketIO] Nhân viên đã đọc tin nhắn của khách: {}", data);
            String customerId = data.get("customerId") != null ? data.get("customerId").toString() : null;
            Long hotelId = null;
            if (data.get("hotelId") != null) {
                try {
                    hotelId = Long.parseLong(data.get("hotelId").toString());
                } catch (NumberFormatException ignored) {}
            }
            if (customerId != null) {
                try {
                    liveChatService.markMessagesAsRead(customerId, hotelId);
                } catch (Exception e) {
                    log.error("Lỗi khi cập nhật đã đọc trong DB: ", e);
                }
                server.getRoomOperations("user_" + customerId).sendEvent("customer_chat_messages_read", data);
                server.getRoomOperations(customerId).sendEvent("customer_chat_messages_read", data);
            }
        });

        server.addEventListener("staff_send_customer_chat", java.util.Map.class, (client, data, ackSender) -> {
            log.info("💬 [SocketIO] Nhân viên phản hồi khách hàng: {}", data);

            try {
                String customerId = data.get("customerId") != null ? data.get("customerId").toString() : null;
                String staffId = data.get("staffId") != null ? data.get("staffId").toString() : null;
                String staffName = data.get("staffName") != null ? data.get("staffName").toString() : null;
                String branchName = data.get("branchName") != null ? data.get("branchName").toString() : null;
                String message = data.get("message") != null ? data.get("message").toString() : null;
                String msgId = data.get("msgId") != null ? data.get("msgId").toString() : null;
                Long hotelId = null;
                if (data.get("hotelId") != null) {
                    try {
                        hotelId = Long.parseLong(data.get("hotelId").toString());
                    } catch (NumberFormatException ignored) {}
                }

                if (message != null && !message.trim().isEmpty()) {
                    liveChatService.saveStaffMessage(
                            customerId, staffId, staffName, hotelId, branchName, message, msgId
                    );
                }
            } catch (Exception e) {
                log.error("Lỗi khi lưu tin nhắn nhân viên vào PostgreSQL: ", e);
            }

            Object customerId = data.get("customerId");
            if (customerId != null && !customerId.toString().trim().isEmpty()) {
                server.getRoomOperations("user_" + customerId).sendEvent("staff_customer_chat_reply", data);
                server.getRoomOperations(customerId.toString()).sendEvent("staff_customer_chat_reply", data);
            }
            Object hotelId = data.get("hotelId");
            if (hotelId != null && !hotelId.toString().trim().isEmpty() && !"null".equalsIgnoreCase(hotelId.toString())) {
                server.getRoomOperations("hotel_" + hotelId).sendEvent("staff_customer_chat_reply", data);
            }
        });

        log.info("SocketIO Server started with customer-staff chat listeners");

        return server;
    }
}

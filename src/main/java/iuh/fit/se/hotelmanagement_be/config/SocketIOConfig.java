package iuh.fit.se.hotelmanagement_be.config;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class SocketIOConfig {
    @Bean
    public JwtDecoder jwtDecoder() {
        // Đổi chuỗi bên dưới thành đúng cái Secret Key ký JWT trong file application.yml/properties của ông
        // Hoặc tạm thời để một chuỗi bất kỳ dài trên 32 ký tự để test
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
//        config.setOrigin("https://tobochat-next.vercel.app");
        config.setOrigin("*");

        config.setAuthorizationListener(handshakeData -> {
            String token = handshakeData.getSingleUrlParam("token");

            if (token != null && !token.isEmpty()) {
                try {
                    Jwt jwt = jwtDecoder().decode(token);
                    log.info("Xác thực thành công cho user: {}", jwt.getSubject());
                    return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;

                } catch (JwtException e) {
                    log.error("Lỗi token: {}", e.getMessage());
                    return AuthorizationResult.FAILED_AUTHORIZATION;
                }
            }
            log.warn("Thiếu token");
            return AuthorizationResult.FAILED_AUTHORIZATION;
        });

        SocketIOServer server = new SocketIOServer(config);

        server.addEventListener("join_user_room", String.class, (client, userId, ackSender) -> {
            if (userId != null && !userId.trim().isEmpty()) {
                client.joinRoom(userId);
                log.info("User {} joined their personal socket room", userId);
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
        log.info("SocketIO Server started with join_user_room listener");

        return server;
    }
}
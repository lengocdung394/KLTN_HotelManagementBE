package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250655368566D5971";
    private static final long EXPIRATION_TIME = 86400000; // 1 ngày

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(Account account) {
        // 1. Tạo một Map chứa các thông tin tùy biến muốn nhét thêm vào Token JWT
        Map<String, Object> extraClaims = new HashMap<>();

        // Kiểm tra an toàn xem đối tượng user liên kết có bị null không trước khi bốc thông tin cá nhân
        if (account.getUser() != null) {
            extraClaims.put("fullName", account.getUser().getFullName());
            extraClaims.put("phone", account.getUser().getPhone());
            extraClaims.put("position", account.getUser().getPosition()); // Nhét thêm chức vụ nếu FE cần
        }

        //  Đút thêm danh sách Roles/Permissions vào Token để Frontend dễ dàng bốc ra kiểm tra quyền ẩn/hiện menu
        if (account.getRoles() != null) {
            List<String> roles = account.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());
            extraClaims.put("roles", roles);
        }

        // 2. Build Token kèm theo Claims
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(account.getEmail()) // LẤY EMAIL TỪ ACCOUNT: Làm chuỗi định danh chủ thể Token
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 1. Trích xuất Username/Email từ Token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 2. Kiểm tra Token có hợp lệ không
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

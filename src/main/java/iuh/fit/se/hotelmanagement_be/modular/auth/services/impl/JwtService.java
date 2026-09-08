package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
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
        Map<String, Object> extraClaims = new HashMap<>();

        // 1. Xử lý trường hợp là Nhân viên / Admin
        if (account.getEmployee() != null) {
            Employee emp = account.getEmployee();
            extraClaims.put("fullName", emp.getFullName());
            extraClaims.put("phone", emp.getPhone());
            extraClaims.put("position", emp.getPosition());

            // Lấy thông tin Hotel trực thuộc
            if (emp.getHotel() != null) {
                extraClaims.put("hotelId", emp.getHotel().getId());
                extraClaims.put("hotelName", emp.getHotel().getName());
            } else {
                // Super Admin (không thuộc chi nhánh nào)
                extraClaims.put("hotelId", null);
                extraClaims.put("hotelName", "Toàn hệ thống");
            }
        }
        // 2. Xử lý trường hợp là Khách hàng
        else if (account.getCustomer() != null) {
            Customer cust = account.getCustomer();
            extraClaims.put("fullName", cust.getFullName());
            extraClaims.put("phone", cust.getPhone());
            extraClaims.put("position", "Khách hàng");
            extraClaims.put("hotelId", null);
            extraClaims.put("hotelName", null);
        }

        // 3. Đút danh sách Roles vào Token cho Frontend kiểm tra quyền
        if (account.getRoles() != null) {
            List<String> roles = account.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());
            extraClaims.put("roles", roles);
        }

        // 4. Build Token
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(account.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 1. Trích xuất Username/Email từ Token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractHotelId(String token) {
        return extractClaim(token, claims -> claims.get("hotelId", Long.class));
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
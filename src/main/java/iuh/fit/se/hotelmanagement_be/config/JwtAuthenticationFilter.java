package iuh.fit.se.hotelmanagement_be.config;

import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    JwtService jwtService;
    UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/auth/")
                || path.startsWith("/api/v1/auth/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lấy chuỗi Authorization từ Header gửi lên
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Nếu không có Header hoặc không bắt đầu bằng "Bearer ", bỏ qua (cho đi tiếp)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Tách lấy chuỗi JWT (bỏ 7 ký tự chữ "Bearer ")
        jwt = authHeader.substring(7);

        // 4. Giải mã JWT để lấy email người dùng
        userEmail = jwtService.extractUsername(jwt);

        // 5. Nếu lấy được email và người dùng chưa được xác thực trong phiên làm việc này
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            System.out.println(">>> userEmail from token: " + userEmail);
            System.out.println(">>> userDetails.getUsername(): " + userDetails.getUsername());
            System.out.println(">>> isTokenValid: " + jwtService.isTokenValid(jwt, userDetails));
            // 6. Kiểm tra xem Token có chuẩn và chưa hết hạn không
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Xác nhận người dùng hợp lệ vào Spring Security Context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 8. Chuyển request sang Filter tiếp theo
        filterChain.doFilter(request, response);
    }
}
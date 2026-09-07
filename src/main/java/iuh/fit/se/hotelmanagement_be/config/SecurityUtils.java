package iuh.fit.se.hotelmanagement_be.config;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    /**
     * Lấy hotelId của User đang đăng nhập hiện tại từ SecurityContext.
     * @return Long (hotelId) nếu là Admin chi nhánh / Nhân viên.
     * @return null nếu là Super Admin (Admin tổng) hoặc chưa đăng nhập.
     */
    public static Long getCurrentUserHotelId() {
        // 1. Lấy đối tượng Authentication từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // 2. Lấy Principal (đối tượng UserDetails/Account đã lưu lúc giải mã JWT)
        Object principal = authentication.getPrincipal();

        // 3. Ép kiểu về Account (vì Account của bạn implements UserDetails)
        if (principal instanceof Account account) {
            return account.getHotelId(); // Hàm getHotelId() bạn đã viết trong Account entity
        }

        return null;
    }

    /**
     * (Tùy chọn) Lấy email của User đang login
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Account account) {
            return account.getEmail();
        }
        return null;
    }
}

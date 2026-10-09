package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RoomRedisLockService {

    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "room_lock_date:";

    /**
     * Giữ chỗ tạm thời cho phòng theo khoảng ngày với thời gian tự hủy (TTL) tùy chỉnh
     * Key dạng: room_lock_date:{roomId}:{checkIn}:{checkOut}
     */
    public boolean lockRoomByDateRange(String roomId, LocalDate checkIn, LocalDate checkOut, String userId, long timeoutMinutes) {
        String key = LOCK_PREFIX + roomId + ":" + checkIn + ":" + checkOut;

        // Redis sẽ tự động xóa key này sau đúng timeoutMinutes nếu không được gia hạn hoặc xóa thủ công
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, userId, Duration.ofMinutes(timeoutMinutes));

        return Boolean.TRUE.equals(success);
    }

    /**
     * Xóa khóa Redis khi hoàn tất thanh toán hoặc hủy
     */
    public void releaseRoomLockByDateRange(String roomId, LocalDate checkIn, LocalDate checkOut) {
        String key = LOCK_PREFIX + roomId + ":" + checkIn + ":" + checkOut;
        redisTemplate.delete(key);
    }
}

package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomRedisLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "room_lock_date:";

    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT =
            new DefaultRedisScript<>(
                    "for i = 1, #KEYS do " +
                            "  if redis.call('EXISTS', KEYS[i]) == 1 then return 0 end " +
                            "end " +
                            "for i = 1, #KEYS do " +
                            "  redis.call('SET', KEYS[i], ARGV[1], 'EX', ARGV[2]) " +
                            "end " +
                            "return 1",
                    Long.class
            );

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    "for i = 1, #KEYS do " +
                            "  if redis.call('GET', KEYS[i]) == ARGV[1] then " +
                            "    redis.call('DEL', KEYS[i]) " +
                            "  end " +
                            "end " +
                            "return 1",
                    Long.class
            );

    /**
     * Khóa từng đêm trong khoảng [checkIn, checkOut).
     * Trả về false nếu có ít nhất một đêm đã được giữ.
     */
    public boolean lockRoomByDateRange(
            String roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            String lockToken,
            long timeoutMinutes
    ) {
        validate(roomId, checkIn, checkOut, lockToken, timeoutMinutes);

        List<String> keys = buildKeys(roomId, checkIn, checkOut);
        long ttlSeconds = Duration.ofMinutes(timeoutMinutes).toSeconds();

        Long result = redisTemplate.execute(
                ACQUIRE_SCRIPT,
                keys,
                lockToken,
                String.valueOf(ttlSeconds)
        );

        if (result == null) {
            throw new IllegalStateException("Redis không trả kết quả khi khóa phòng.");
        }

        return result == 1L;
    }

    /**
     * Chỉ mở khóa các key đang thuộc lockToken này.
     */
    public void releaseRoomLockByDateRange(
            String roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            String lockToken
    ) {
        if (roomId == null || roomId.isBlank()
                || checkIn == null || checkOut == null
                || !checkIn.isBefore(checkOut)
                || lockToken == null || lockToken.isBlank()) {
            throw new IllegalArgumentException("Thông tin mở khóa phòng không hợp lệ.");
        }

        Long result = redisTemplate.execute(
                RELEASE_SCRIPT,
                buildKeys(roomId, checkIn, checkOut),
                lockToken
        );

        if (result == null) {
            throw new IllegalStateException("Redis không trả kết quả khi mở khóa phòng.");
        }
    }

    private List<String> buildKeys(String roomId, LocalDate checkIn, LocalDate checkOut) {
        List<String> keys = new ArrayList<>();

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            keys.add(LOCK_PREFIX + "{" + roomId + "}:" + date);
        }

        return keys;
    }

    private void validate(
            String roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            String lockToken,
            long timeoutMinutes
    ) {
        if (roomId == null || roomId.isBlank()
                || lockToken == null || lockToken.isBlank()) {
            throw new IllegalArgumentException("roomId và lockToken không được để trống.");
        }

        if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("Khoảng ngày đặt phòng không hợp lệ.");
        }

        if (timeoutMinutes <= 0) {
            throw new IllegalArgumentException("Thời gian giữ phòng phải lớn hơn 0.");
        }
    }

}

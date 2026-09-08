package iuh.fit.se.hotelmanagement_be.modular.promotion.scheduler;

import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PromotionExpiryScheduler {

    PromotionRepository promotionRepository;

    /**
     * Chạy mỗi ngày lúc 00:05 AM
     * Tự động chuyển các KM đã qua endDate sang trạng thái EXPIRED
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void expireOutdatedPromotions() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[Scheduler] Kiểm tra khuyến mãi hết hạn lúc: {}", now);
        try {
            int count = promotionRepository.bulkExpirePromotions(now);
            if (count > 0) {
                log.info("[Scheduler] Đã expire {} khuyến mãi", count);
            }
        } catch (Exception e) {
            log.error("[Scheduler] Lỗi khi expire khuyến mãi: {}", e.getMessage(), e);
        }
    }
}

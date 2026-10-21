package iuh.fit.se.hotelmanagement_be.modular.shift.scheduler;

import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShiftScheduleReminderScheduler {
    private final HotelRepository hotelRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 15 * * SAT", zone = "Asia/Ho_Chi_Minh")
    public void remindManagersToPlanNextWeek() {
        LocalDate nextMonday = LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        for (Hotel hotel : hotelRepository.findAll()) {
            List<String> managers = accountRepository.findAccountIdsByHotelIdAndRoleCodeIn(
                    hotel.getId(), List.of("ROLE_MANAGER"));
            for (String managerAccountId : managers) {
                notificationService.createAndPublishToAccount(
                        managerAccountId,
                        hotel.getId(),
                        NotificationType.SHIFT_SCHEDULE_REMINDER,
                        "Lập lịch ca làm tuần tới",
                        "Vui lòng phân công lịch làm việc cho tuần bắt đầu ngày " + nextMonday + ".",
                        "SHIFT_WEEK",
                        nextMonday.toString());
            }
            if (managers.isEmpty()) {
                log.warn("No manager account found for hotel {} when sending weekly shift reminder", hotel.getId());
            }
        }
    }
}

package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HousekeepingTaskScheduler {
    private final HousekeepingTaskService housekeepingTaskService;

    @Scheduled(fixedDelay = 60000)
    public void reassignOverdueTasks() {
        housekeepingTaskService.reassignOverdueTasks();
    }
}

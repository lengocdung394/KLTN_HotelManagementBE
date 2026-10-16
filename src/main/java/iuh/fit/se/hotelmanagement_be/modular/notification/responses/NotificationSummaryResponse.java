package iuh.fit.se.hotelmanagement_be.modular.notification.responses;

import java.util.List;
import java.util.Map;

public record NotificationSummaryResponse(
        long totalCount,
        long readCount,
        long unreadCount,
        Map<String, Long> totalByType,
        Map<String, Long> unreadByType,
        List<BranchNotificationCount> branches
) {
    public record BranchNotificationCount(Long hotelId, long totalCount, long readCount, long unreadCount) {
    }
}

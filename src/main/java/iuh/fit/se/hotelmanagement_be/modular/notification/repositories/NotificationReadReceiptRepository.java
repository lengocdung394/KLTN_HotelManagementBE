package iuh.fit.se.hotelmanagement_be.modular.notification.repositories;

import iuh.fit.se.hotelmanagement_be.modular.notification.entities.NotificationReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationReadReceiptRepository extends JpaRepository<NotificationReadReceipt, Long> {
    Optional<NotificationReadReceipt> findByNotification_IdAndAccountId(Long notificationId, String accountId);

    List<NotificationReadReceipt> findByAccountIdInAndNotification_IdIn(
            Collection<String> accountIds,
            Collection<Long> notificationIds);
}

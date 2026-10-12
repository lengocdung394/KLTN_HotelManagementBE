package iuh.fit.se.hotelmanagement_be.modular.notification.repositories;

import iuh.fit.se.hotelmanagement_be.modular.notification.entities.Notification;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE ((:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter))
                OR (:superAdmin = false AND n.hotelId = :hotelFilter))
              AND (:type IS NULL OR n.type = :type)
              AND (:unreadOnly = false OR NOT EXISTS (
                    SELECT receipt.id FROM NotificationReadReceipt receipt
                    WHERE receipt.notification = n AND receipt.accountId = :accountId
              ))
            """)
    Page<Notification> findVisibleNotifications(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter,
            @Param("type") NotificationType type,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("accountId") String accountId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE ((:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter))
                OR (:superAdmin = false AND n.hotelId = :hotelFilter))
              AND NOT EXISTS (
                    SELECT receipt.id FROM NotificationReadReceipt receipt
                    WHERE receipt.notification = n AND receipt.accountId = :accountId
              )
            """)
    long countUnread(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter,
            @Param("accountId") String accountId);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE (:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter))
               OR (:superAdmin = false AND n.hotelId = :hotelFilter)
            """)
    long countVisible(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter);

    @Query("""
            SELECT n.type AS key, COUNT(n) AS total
            FROM Notification n
            WHERE ((:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter))
                OR (:superAdmin = false AND n.hotelId = :hotelFilter))
            GROUP BY n.type
            """)
    List<NotificationCountProjection> countByType(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter);

    @Query("""
            SELECT n.type AS key, COUNT(n) AS total
            FROM Notification n
            WHERE ((:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter))
                OR (:superAdmin = false AND n.hotelId = :hotelFilter))
              AND NOT EXISTS (
                    SELECT receipt.id FROM NotificationReadReceipt receipt
                    WHERE receipt.notification = n AND receipt.accountId = :accountId
              )
            GROUP BY n.type
            """)
    List<NotificationCountProjection> countUnreadByType(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter,
            @Param("accountId") String accountId);

    @Query("""
            SELECT n.hotelId AS key, COUNT(n) AS total
            FROM Notification n
            WHERE n.hotelId IS NOT NULL
            GROUP BY n.hotelId
            """)
    List<NotificationCountProjection> countByHotel();

    @Query("""
            SELECT n.hotelId AS key, COUNT(n) AS total
            FROM Notification n
            WHERE n.hotelId IS NOT NULL
              AND NOT EXISTS (
                    SELECT receipt.id FROM NotificationReadReceipt receipt
                    WHERE receipt.notification = n AND receipt.accountId = :accountId
              )
            GROUP BY n.hotelId
            """)
    List<NotificationCountProjection> countUnreadByHotel(@Param("accountId") String accountId);

    @Query("""
            SELECT n FROM Notification n
            WHERE ((:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter))
                OR (:superAdmin = false AND n.hotelId = :hotelFilter))
              AND NOT EXISTS (
                    SELECT receipt.id FROM NotificationReadReceipt receipt
                    WHERE receipt.notification = n AND receipt.accountId = :accountId
              )
            """)
    List<Notification> findUnreadVisible(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter,
            @Param("accountId") String accountId);

    interface NotificationCountProjection {
        Object getKey();
        Long getTotal();
    }
}

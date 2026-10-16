package iuh.fit.se.hotelmanagement_be.modular.notification.repositories;

import iuh.fit.se.hotelmanagement_be.modular.notification.entities.Notification;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE (
                    (:customer = true AND n.recipientAccountId = :accountId)
                    OR (:customer = false AND (
                        (:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                        OR (:superAdmin = false AND (n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                    ))
                  )
              AND (
                    (:customer = true)
                    OR (:customer = false AND (
                        (:superAdmin = true AND n.visibleToSuperAdmin = true)
                        OR (:superAdmin = false AND n.visibleToHotel = true)
                    ))
                  )
              AND (:type IS NULL OR n.type = :type)
              AND (
                    :unreadOnly = false
                    OR NOT EXISTS (
                        SELECT receipt.id FROM NotificationReadReceipt receipt
                        WHERE receipt.notification = n
                          AND receipt.accountId IN :readScopeKeys
                    )
                  )
            """)
    Page<Notification> findVisibleNotifications(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter,
            @Param("customer") boolean customer,
            @Param("type") NotificationType type,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("accountId") String accountId,
            @Param("readScopeKeys") List<String> readScopeKeys,
            Pageable pageable);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE (
                    (:customer = true AND n.recipientAccountId = :accountId)
                    OR (:customer = false AND (
                        (:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                        OR (:superAdmin = false AND (n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                    ))
                  )
              AND (
                    (:customer = true)
                    OR (:customer = false AND (
                        (:superAdmin = true AND n.visibleToSuperAdmin = true)
                        OR (:superAdmin = false AND n.visibleToHotel = true)
                    ))
                  )
              AND NOT EXISTS (
                    SELECT receipt.id FROM NotificationReadReceipt receipt
                    WHERE receipt.notification = n
                      AND receipt.accountId IN :readScopeKeys
                  )
            """)
    long countUnread(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter,
            @Param("customer") boolean customer,
            @Param("accountId") String accountId,
            @Param("readScopeKeys") List<String> readScopeKeys);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE (
                    (:customer = true AND n.recipientAccountId = :accountId)
                    OR (:customer = false AND (
                        (:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                        OR (:superAdmin = false AND (n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                    ))
                  )
              AND (
                    (:customer = true)
                    OR (:customer = false AND (
                        (:superAdmin = true AND n.visibleToSuperAdmin = true)
                        OR (:superAdmin = false AND n.visibleToHotel = true)
                    ))
                  )
            """)
    long countVisible(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter,
            @Param("customer") boolean customer,
            @Param("accountId") String accountId);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.hotelId IS NULL
              AND n.visibleToSuperAdmin = true
            """)
    long countGlobalVisible();

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.hotelId IS NULL
              AND n.visibleToSuperAdmin = true
              AND NOT EXISTS (
                    SELECT receipt.id FROM NotificationReadReceipt receipt
                    WHERE receipt.notification = n
                      AND receipt.accountId IN :readScopeKeys
                  )
            """)
    long countGlobalUnread(@Param("readScopeKeys") List<String> readScopeKeys);

    @Query("""
            SELECT n.type AS key, COUNT(n) AS total
            FROM Notification n
            WHERE (
                    (:customer = true AND n.recipientAccountId = :accountId)
                    OR (:customer = false AND (
                        (:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                        OR (:superAdmin = false AND (n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                    ))
                  )
              AND (
                    (:customer = true)
                    OR (:customer = false AND (
                        (:superAdmin = true AND n.visibleToSuperAdmin = true)
                        OR (:superAdmin = false AND n.visibleToHotel = true)
                    ))
                  )
            GROUP BY n.type
            """)
    List<NotificationCountProjection> countByType(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter,
            @Param("customer") boolean customer,
            @Param("accountId") String accountId);

    @Query("""
            SELECT n.type AS key, COUNT(n) AS total
            FROM Notification n
            WHERE (
                    (:customer = true AND n.recipientAccountId = :accountId)
                    OR (:customer = false AND (
                        (:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                        OR (:superAdmin = false AND (n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                    ))
                  )
              AND (
                    (:customer = true)
                    OR (:customer = false AND (
                        (:superAdmin = true AND n.visibleToSuperAdmin = true)
                        OR (:superAdmin = false AND n.visibleToHotel = true)
                    ))
                  )
              AND NOT EXISTS (
                    SELECT receipt.id FROM NotificationReadReceipt receipt
                    WHERE receipt.notification = n
                      AND receipt.accountId IN :readScopeKeys
                  )
            GROUP BY n.type
            """)
    List<NotificationCountProjection> countUnreadByType(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter,
            @Param("customer") boolean customer,
            @Param("accountId") String accountId,
            @Param("readScopeKeys") List<String> readScopeKeys);

    @Query("""
            SELECT n.hotelId AS key, COUNT(n) AS total
            FROM Notification n
            WHERE n.hotelId IS NOT NULL
              AND n.visibleToSuperAdmin = true
            GROUP BY n.hotelId
            """)
    List<NotificationCountProjection> countByHotel();

    @Query("""
            SELECT n.hotelId AS key, COUNT(n) AS total
            FROM Notification n
            WHERE n.hotelId IS NOT NULL
              AND n.visibleToSuperAdmin = true
              AND NOT EXISTS (
                    SELECT receipt.id FROM NotificationReadReceipt receipt
                    WHERE receipt.notification = n
                      AND receipt.accountId IN :readScopeKeys
                  )
            GROUP BY n.hotelId
            """)
    List<NotificationCountProjection> countUnreadByHotel(
            @Param("readScopeKeys") List<String> readScopeKeys);

    @Query("""
            SELECT n FROM Notification n
            WHERE (
                    (:customer = true AND n.recipientAccountId = :accountId)
                    OR (:customer = false AND (
                        (:superAdmin = true AND (:hotelFilter IS NULL OR n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                        OR (:superAdmin = false AND (n.hotelId = :hotelFilter OR n.hotelId IS NULL))
                    ))
                  )
              AND (
                    (:customer = true)
                    OR (:customer = false AND (
                        (:superAdmin = true AND n.visibleToSuperAdmin = true)
                        OR (:superAdmin = false AND n.visibleToHotel = true)
                    ))
                  )
              AND NOT EXISTS (
                    SELECT receipt.id FROM NotificationReadReceipt receipt
                    WHERE receipt.notification = n
                      AND receipt.accountId IN :readScopeKeys
                  )
            """)
    List<Notification> findUnreadVisible(
            @Param("superAdmin") boolean superAdmin,
            @Param("hotelFilter") Long hotelFilter,
            @Param("customer") boolean customer,
            @Param("accountId") String accountId,
            @Param("readScopeKeys") List<String> readScopeKeys);

    interface NotificationCountProjection {
        Object getKey();

        Long getTotal();
    }
}

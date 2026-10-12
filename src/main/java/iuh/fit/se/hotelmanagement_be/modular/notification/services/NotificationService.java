package iuh.fit.se.hotelmanagement_be.modular.notification.services;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.Notification;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.NotificationReadReceipt;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.repositories.NotificationReadReceiptRepository;
import iuh.fit.se.hotelmanagement_be.modular.notification.repositories.NotificationRepository;
import iuh.fit.se.hotelmanagement_be.modular.notification.responses.NotificationPageResponse;
import iuh.fit.se.hotelmanagement_be.modular.notification.responses.NotificationResponse;
import iuh.fit.se.hotelmanagement_be.modular.notification.responses.NotificationSocketPayload;
import iuh.fit.se.hotelmanagement_be.modular.notification.responses.NotificationSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private static final String SUPER_ADMIN_ROOM = "super_admin_accounts";

    private final NotificationRepository notificationRepository;
    private final NotificationReadReceiptRepository receiptRepository;
    private final SocketIOServer socketIOServer;

    @Transactional
    public Notification createAndPublish(
            Long hotelId,
            NotificationType type,
            String title,
            String description,
            String referenceType,
            String referenceId) {
        Notification notification = new Notification();
        notification.setHotelId(hotelId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        Notification saved = notificationRepository.save(notification);
        NotificationSocketPayload payload = toSocketPayload(saved);

        Runnable emit = () -> {
            try {
                if (hotelId != null) {
                    socketIOServer.getRoomOperations("hotel_" + hotelId)
                            .sendEvent("notification_created", payload);
                }
                socketIOServer.getRoomOperations(SUPER_ADMIN_ROOM)
                        .sendEvent("notification_created", payload);
            } catch (RuntimeException exception) {
                log.error("Could not emit persisted notification {}", saved.getId(), exception);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emit.run();
                }
            });
        } else {
            emit.run();
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(
            Account account,
            Long requestedHotelId,
            NotificationType type,
            boolean unreadOnly,
            int page,
            int size) {
        boolean superAdmin = isSuperAdmin(account);
        Long hotelFilter = resolveHotelFilter(account, requestedHotelId, superAdmin);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Page<Notification> results = notificationRepository.findVisibleNotifications(
                superAdmin,
                hotelFilter,
                type,
                unreadOnly,
                account.getId(),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        Set<Long> ids = results.getContent().stream().map(Notification::getId).collect(Collectors.toSet());
        Set<Long> readIds = receiptsFor(account, ids).stream()
                .map(receipt -> receipt.getNotification().getId())
                .collect(Collectors.toSet());
        List<NotificationResponse> content = results.getContent().stream()
                .map(notification -> toResponse(notification, readIds.contains(notification.getId())))
                .toList();
        return new NotificationPageResponse(
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages(),
                content);
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse getSummary(Account account, Long requestedHotelId) {
        boolean superAdmin = isSuperAdmin(account);
        Long hotelFilter = resolveHotelFilter(account, requestedHotelId, superAdmin);
        String accountId = account.getId();

        Map<String, Long> totalByType = toCountMap(notificationRepository.countByType(superAdmin, hotelFilter));
        Map<String, Long> unreadByType = toCountMap(
                notificationRepository.countUnreadByType(superAdmin, hotelFilter, accountId));
        long totalCount = notificationRepository.countVisible(superAdmin, hotelFilter);
        long unreadCount = notificationRepository.countUnread(superAdmin, hotelFilter, accountId);
        List<NotificationSummaryResponse.BranchNotificationCount> branches = List.of();
        if (superAdmin && hotelFilter == null) {
            Map<Long, Long> totals = toLongCountMap(notificationRepository.countByHotel());
            Map<Long, Long> unread = toLongCountMap(notificationRepository.countUnreadByHotel(accountId));
            branches = totals.entrySet().stream()
                    .map(entry -> new NotificationSummaryResponse.BranchNotificationCount(
                            entry.getKey(), entry.getValue(), unread.getOrDefault(entry.getKey(), 0L)))
                    .toList();
        } else if (hotelFilter != null) {
            branches = List.of(new NotificationSummaryResponse.BranchNotificationCount(
                    hotelFilter, totalCount, unreadCount));
        }
        return new NotificationSummaryResponse(totalCount, unreadCount, totalByType, unreadByType, branches);
    }

    @Transactional
    public NotificationResponse markAsRead(Account account, Long notificationId) {
        boolean superAdmin = isSuperAdmin(account);
        Long hotelFilter = resolveHotelFilter(account, null, superAdmin);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!isVisible(notification, hotelFilter, superAdmin)) {
            throw new AppException(ErrorCode.NOTIFICATION_SCOPE_INVALID);
        }
        if (receiptRepository.findByNotification_IdAndAccountId(notificationId, account.getId()).isEmpty()) {
            receiptRepository.save(new NotificationReadReceipt(notification, account.getId()));
        }
        return toResponse(notification, true);
    }

    @Transactional
    public long markAllAsRead(Account account, Long requestedHotelId) {
        boolean superAdmin = isSuperAdmin(account);
        Long hotelFilter = resolveHotelFilter(account, requestedHotelId, superAdmin);
        List<Notification> unread = notificationRepository.findUnreadVisible(
                superAdmin, hotelFilter, account.getId());
        List<NotificationReadReceipt> receipts = unread.stream()
                .map(notification -> new NotificationReadReceipt(notification, account.getId()))
                .toList();
        receiptRepository.saveAll(receipts);
        return receipts.size();
    }

    private List<NotificationReadReceipt> receiptsFor(Account account, Collection<Long> notificationIds) {
        if (notificationIds.isEmpty()) return List.of();
        return receiptRepository.findByAccountIdAndNotification_IdIn(account.getId(), notificationIds);
    }

    private boolean isSuperAdmin(Account account) {
        ensureSupportedRole(account);
        return account.getRoles().stream().anyMatch(role -> "ROLE_SUPER_ADMIN".equals(role.getCode()));
    }

    private void ensureSupportedRole(Account account) {
        if (account == null || account.getId() == null || account.getRoles() == null
                || account.getRoles().stream().noneMatch(role -> Set.of(
                        "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE")
                .contains(role.getCode()))) {
            throw new AppException(ErrorCode.NOTIFICATION_ROLE);
        }
    }

    private Long resolveHotelFilter(Account account, Long requestedHotelId, boolean superAdmin) {
        if (superAdmin) return requestedHotelId;
        Long accountHotelId = account.getHotelId();
        if (accountHotelId == null || (requestedHotelId != null && !requestedHotelId.equals(accountHotelId))) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACCESS);
        }
        return accountHotelId;
    }

    private boolean isVisible(Notification notification, Long hotelFilter, boolean superAdmin) {
        return superAdmin ? hotelFilter == null || hotelFilter.equals(notification.getHotelId())
                : hotelFilter != null && hotelFilter.equals(notification.getHotelId());
    }

    private Map<String, Long> toCountMap(List<NotificationRepository.NotificationCountProjection> rows) {
        Map<String, Long> result = new HashMap<>();
        rows.forEach(row -> result.put(String.valueOf(row.getKey()), row.getTotal()));
        return result;
    }

    private Map<Long, Long> toLongCountMap(List<NotificationRepository.NotificationCountProjection> rows) {
        Map<Long, Long> result = new HashMap<>();
        rows.forEach(row -> result.put(Long.valueOf(String.valueOf(row.getKey())), row.getTotal()));
        return result;
    }

    private NotificationResponse toResponse(Notification notification, boolean read) {
        return new NotificationResponse(
                notification.getId(),
                notification.getHotelId(),
                notification.getType(),
                notification.getTitle(),
                notification.getDescription(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getCreatedAt(),
                read);
    }

    private NotificationSocketPayload toSocketPayload(Notification notification) {
        return new NotificationSocketPayload(
                notification.getId(),
                notification.getHotelId(),
                notification.getType(),
                notification.getTitle(),
                notification.getDescription(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getCreatedAt());
    }
}

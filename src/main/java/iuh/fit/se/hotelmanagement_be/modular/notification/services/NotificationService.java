package iuh.fit.se.hotelmanagement_be.modular.notification.services;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
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
import org.springframework.transaction.annotation.Propagation;
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
    private final AccountRepository accountRepository;
    private final HotelRepository hotelRepository;
    private final SocketIOServer socketIOServer;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createAndPublish(
            Long hotelId,
            NotificationType type,
            String title,
            String description,
            String referenceType,
            String referenceId) {


        return createAndPublish(
                hotelId, type, title, description, referenceType, referenceId,
                true, true
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createAndPublishToCustomer(
            String recipientAccountId,
            Long hotelId,
            NotificationType type,
            String title,
            String description,
            String referenceType,
            String referenceId) {
        if (recipientAccountId == null || recipientAccountId.isBlank()) {
            throw new IllegalArgumentException("Customer account ID must not be blank");
        }
        return createAndPublish(
                hotelId, type, title, description, referenceType, referenceId,
                false, false, recipientAccountId);
    }

    @Transactional
    public Notification createAndPublishSystemWide(
            NotificationType type,
            String title,
            String description,
            String referenceType,
            String referenceId,
            Collection<Long> hotelIds) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setVisibleToHotel(true);
        notification.setVisibleToSuperAdmin(true);

        Notification saved = notificationRepository.save(notification);
        NotificationSocketPayload payload = toSocketPayload(saved);
        List<Long> targetHotelIds = hotelIds == null
                ? List.of()
                : hotelIds.stream().filter(Objects::nonNull).distinct().toList();
        Runnable emit = () -> {
            try {
                targetHotelIds.forEach(hotelId ->
                        socketIOServer.getRoomOperations("hotel_" + hotelId)
                                .sendEvent("notification_created", payload));
                socketIOServer.getRoomOperations(SUPER_ADMIN_ROOM)
                        .sendEvent("notification_created", payload);
            } catch (RuntimeException exception) {
                log.error("Could not emit persisted system-wide notification {}", saved.getId(), exception);
            }
        };
        publishAfterCommit(emit);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createAndPublish(
            Long hotelId,
            NotificationType type,
            String title,
            String description,
            String referenceType,
            String referenceId,
            boolean publishToHotel,
            boolean publishToSuperAdmin) {
        return createAndPublish(
                hotelId, type, title, description, referenceType, referenceId,
                publishToHotel, publishToSuperAdmin, null);
    }

    @Transactional
    public Notification createAndPublishInCurrentTransaction(
            Long hotelId,
            NotificationType type,
            String title,
            String description,
            String referenceType,
            String referenceId) {
        return createAndPublish(
                hotelId, type, title, description, referenceType, referenceId,
                true, true);
    }

    private Notification createAndPublish(
            Long hotelId,
            NotificationType type,
            String title,
            String description,
            String referenceType,
            String referenceId,
            boolean publishToHotel,
            boolean publishToSuperAdmin,
            String recipientAccountId) {
        Notification notification = new Notification();
        notification.setHotelId(hotelId);
        notification.setRecipientAccountId(recipientAccountId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);


        notification.setVisibleToHotel(publishToHotel);
        notification.setVisibleToSuperAdmin(publishToSuperAdmin);


        Notification saved = notificationRepository.save(notification);
        NotificationSocketPayload payload = toSocketPayload(saved);

        Runnable emit = () -> {
            try {
                if (publishToHotel && hotelId != null) {
                    socketIOServer.getRoomOperations("hotel_" + hotelId)
                            .sendEvent("notification_created", payload);
                }

                if (publishToSuperAdmin) {
                    socketIOServer.getRoomOperations(SUPER_ADMIN_ROOM)
                            .sendEvent("notification_created", payload);
                }
                if (recipientAccountId != null) {
                    socketIOServer.getRoomOperations(recipientAccountId)
                            .sendEvent("notification_created", payload);
                }
            } catch (RuntimeException exception) {
                log.error("Could not emit persisted notification {}", saved.getId(), exception);
            }
        };

        publishAfterCommit(emit);

        return saved;
    }

    private void publishAfterCommit(Runnable emit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            emit.run();
                        }
                    });
        } else {
            emit.run();
        }
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
        boolean customer = isCustomer(account);
        String readScopeKey = readScopeKey(account, superAdmin);
        List<String> readScopeKeys = readScopeKeys(account, superAdmin, readScopeKey);
        Page<Notification> results = notificationRepository.findVisibleNotifications(
                superAdmin,
                hotelFilter,
                customer,
                type,
                unreadOnly,
                account.getId(),
                readScopeKeys,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        Set<Long> ids = results.getContent().stream().map(Notification::getId).collect(Collectors.toSet());
        Set<Long> readIds = receiptsFor(readScopeKeys, ids).stream()
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
        boolean customer = isCustomer(account);
        String readScopeKey = readScopeKey(account, superAdmin);
        List<String> readScopeKeys = readScopeKeys(account, superAdmin, readScopeKey);

        Map<String, Long> totalByType = toCountMap(
                notificationRepository.countByType(superAdmin, hotelFilter, customer, accountId));
        Map<String, Long> unreadByType = toCountMap(
                notificationRepository.countUnreadByType(
                        superAdmin, hotelFilter, customer, accountId, readScopeKeys));
        long totalCount = notificationRepository.countVisible(superAdmin, hotelFilter, customer, accountId);
        long unreadCount = notificationRepository.countUnread(
                superAdmin, hotelFilter, customer, accountId, readScopeKeys);
        long readCount = totalCount - unreadCount;
        List<NotificationSummaryResponse.BranchNotificationCount> branches = List.of();
        if (superAdmin && hotelFilter == null) {
            Map<Long, Long> totals = toLongCountMap(notificationRepository.countByHotel());
            Map<Long, Long> unread = toLongCountMap(notificationRepository.countUnreadByHotel(readScopeKeys));
            long globalTotal = notificationRepository.countGlobalVisible();
            long globalUnread = notificationRepository.countGlobalUnread(readScopeKeys);
            branches = hotelRepository.findAll().stream()
                    .map(hotel -> {
                        long branchTotal = totals.getOrDefault(hotel.getId(), 0L) + globalTotal;
                        long branchUnread = unread.getOrDefault(hotel.getId(), 0L) + globalUnread;
                        return new NotificationSummaryResponse.BranchNotificationCount(
                                hotel.getId(),
                                branchTotal,
                                branchTotal - branchUnread,
                                branchUnread);
                    })
                    .toList();
        } else if (hotelFilter != null) {
            branches = List.of(new NotificationSummaryResponse.BranchNotificationCount(
                    hotelFilter, totalCount, readCount, unreadCount));
        }
        return new NotificationSummaryResponse(
                totalCount, readCount, unreadCount, totalByType, unreadByType, branches);
    }

    @Transactional
    public NotificationResponse markAsRead(Account account, Long notificationId) {
        boolean superAdmin = isSuperAdmin(account);
        Long hotelFilter = resolveHotelFilter(account, null, superAdmin);
        String readScopeKey = readScopeKey(account, superAdmin);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!isVisible(notification, hotelFilter, superAdmin, isCustomer(account), account.getId())) {
            throw new AppException(ErrorCode.NOTIFICATION_SCOPE_INVALID);
        }
        if (receiptRepository.findByNotification_IdAndAccountId(notificationId, readScopeKey).isEmpty()) {
            receiptRepository.save(new NotificationReadReceipt(notification, readScopeKey));
        }
        return toResponse(notification, true);
    }

    @Transactional
    public long markAllAsRead(Account account, Long requestedHotelId) {
        boolean superAdmin = isSuperAdmin(account);
        Long hotelFilter = resolveHotelFilter(account, requestedHotelId, superAdmin);
        String readScopeKey = readScopeKey(account, superAdmin);
        List<String> readScopeKeys = readScopeKeys(account, superAdmin, readScopeKey);
        boolean customer = isCustomer(account);
        List<Notification> unread = notificationRepository.findUnreadVisible(
                superAdmin, hotelFilter, customer, account.getId(), readScopeKeys);
        List<NotificationReadReceipt> receipts = unread.stream()
                .map(notification -> new NotificationReadReceipt(notification, readScopeKey))
                .toList();
        receiptRepository.saveAll(receipts);
        return receipts.size();
    }

    private List<NotificationReadReceipt> receiptsFor(List<String> readScopeKeys, Collection<Long> notificationIds) {
        if (notificationIds.isEmpty()) return List.of();
        return receiptRepository.findByAccountIdInAndNotification_IdIn(readScopeKeys, notificationIds);
    }

    private boolean isSuperAdmin(Account account) {
        ensureSupportedRole(account);
        return account.getRoles().stream().anyMatch(role -> "ROLE_SUPER_ADMIN".equals(role.getCode()));
    }

    private void ensureSupportedRole(Account account) {
        if (account == null || account.getId() == null || account.getRoles() == null
                || account.getRoles().stream().noneMatch(role -> Set.of(
                        "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_RECEPTIONIST", "ROLE_STAFF", "ROLE_CUSTOMER")
                .contains(role.getCode()))) {
            throw new AppException(ErrorCode.NOTIFICATION_ROLE);
        }
    }

    private Long resolveHotelFilter(Account account, Long requestedHotelId, boolean superAdmin) {
        if (superAdmin) return requestedHotelId;
        if (isCustomer(account)) {
            if (requestedHotelId != null) {
                throw new AppException(ErrorCode.ACCOUNT_NOT_ACCESS);
            }
            return null;
        }
        Long accountHotelId = account.getHotelId();
        if (accountHotelId == null || (requestedHotelId != null && !requestedHotelId.equals(accountHotelId))) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACCESS);
        }
        return accountHotelId;
    }

    private boolean isVisible(
            Notification notification,
            Long hotelFilter,
            boolean superAdmin,
            boolean customer,
            String accountId
    ) {
        if (customer) {
            return accountId.equals(notification.getRecipientAccountId());
        }
        if (superAdmin) {
            return notification.isVisibleToSuperAdmin()
                    && (hotelFilter == null || hotelFilter.equals(notification.getHotelId())
                    || notification.getHotelId() == null);
        }
        return notification.isVisibleToHotel()
                && hotelFilter != null
                && (hotelFilter.equals(notification.getHotelId()) || notification.getHotelId() == null);
    }

    private boolean isCustomer(Account account) {
        return account.getRoles().stream()
                .anyMatch(role -> "ROLE_CUSTOMER".equals(role.getCode()));
    }

    private String readScopeKey(Account account, boolean superAdmin) {
        if (superAdmin) {
            return "SUPER_ADMIN";
        }
        if (isCustomer(account)) {
            return "CUSTOMER:" + account.getId();
        }
        return isBranchManagement(account)
                ? "HOTEL_MANAGEMENT:" + account.getHotelId()
                : "HOTEL_EMPLOYEE:" + account.getHotelId();
    }

    private List<String> readScopeKeys(Account account, boolean superAdmin, String readScopeKey) {
        List<String> keys = new ArrayList<>();
        keys.add(readScopeKey);
        if (superAdmin) {
            keys.addAll(accountRepository.findSuperAdminAccountIds());
        } else if (!isCustomer(account)) {
            if (isBranchManagement(account)) {
                keys.add("HOTEL:" + account.getHotelId());
                keys.addAll(accountRepository.findAccountIdsByHotelIdAndRoleCodeIn(
                        account.getHotelId(), List.of("ROLE_ADMIN", "ROLE_MANAGER")));
            } else {
                keys.addAll(accountRepository.findAccountIdsByHotelIdAndRoleCodeIn(
                        account.getHotelId(), List.of("ROLE_EMPLOYEE", "ROLE_RECEPTIONIST", "ROLE_STAFF")));
            }
        } else {
            keys.add(account.getId());
        }
        return keys.stream().distinct().toList();
    }

    private boolean isBranchManagement(Account account) {
        return account.getRoles().stream()
                .anyMatch(role -> Set.of("ROLE_ADMIN", "ROLE_MANAGER").contains(role.getCode()));
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
                notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : java.time.LocalDateTime.now().toString());
    }
}

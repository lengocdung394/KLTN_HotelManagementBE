package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountSocketEmitter {

    private static final String SUPER_ADMIN_ROOM = "super_admin_accounts";

    private final SocketIOServer socketIOServer;
    // tao tai khoan
    public void emitAccountCreated(String accountId, String profileId, String accountType, Long hotelId) {
        AccountCreatedPayload payload = new AccountCreatedPayload(
                accountId, profileId, accountType, hotelId, Instant.now());
        afterCommit(() -> socketIOServer.getRoomOperations(SUPER_ADMIN_ROOM)
                .sendEvent("account_created", payload));
    }



    public void emitBranchCreated(Long hotelId, String name) {
        BranchCreatedPayload payload = new BranchCreatedPayload(hotelId, name, Instant.now());
        afterCommit(() -> socketIOServer.getRoomOperations(SUPER_ADMIN_ROOM)
                .sendEvent("branch_created", payload));
    }

    private void afterCommit(Runnable emit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emit.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emit.run();
            }
        });
    }

    private record AccountCreatedPayload(
            String accountId,
            String profileId,
            String accountType,
            Long hotelId,
            Instant createdAt) {
    }

    private record BranchCreatedPayload(Long hotelId, String name, Instant createdAt) {
    }

    // Tao tai khoan cho khach hang
    public void emitGuestCustomerCreated(String customerId, Long hotelId) {
        GuestCustomerCreatedPayload payload = new GuestCustomerCreatedPayload(
                customerId, hotelId, Instant.now());
        afterCommit(() -> socketIOServer.getRoomOperations(SUPER_ADMIN_ROOM)
                .sendEvent("customer_created", payload));
    }

    private record GuestCustomerCreatedPayload(String customerId, Long hotelId, Instant createdAt) {
    }
}

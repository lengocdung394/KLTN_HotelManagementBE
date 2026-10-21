package iuh.fit.se.hotelmanagement_be.modular.chat.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.chat.entities.ChatMessage;
import iuh.fit.se.hotelmanagement_be.modular.chat.entities.ChatSession;
import iuh.fit.se.hotelmanagement_be.modular.chat.enums.ChatSenderType;
import iuh.fit.se.hotelmanagement_be.modular.chat.enums.ChatSessionStatus;
import iuh.fit.se.hotelmanagement_be.modular.chat.repositories.ChatMessageRepository;
import iuh.fit.se.hotelmanagement_be.modular.chat.repositories.ChatSessionRepository;
import iuh.fit.se.hotelmanagement_be.modular.chat.responses.ChatMessageResponse;
import iuh.fit.se.hotelmanagement_be.modular.chat.responses.ChatSessionResponse;
import iuh.fit.se.hotelmanagement_be.modular.chat.services.LiveChatService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LiveChatServiceImpl implements LiveChatService {

    ChatSessionRepository sessionRepository;
    ChatMessageRepository messageRepository;

    @Override
    @Transactional
    public ChatMessageResponse saveCustomerMessage(
            String customerId,
            String customerName,
            String customerPhone,
            Long hotelId,
            String branchName,
            String message,
            String msgId
    ) {
        if (customerId == null || customerId.isBlank()) {
            customerId = "guest_anonymous";
        }

        // Chống lặp tin nhắn nếu msgId đã tồn tại trong DB
        if (msgId != null && !msgId.isBlank() && messageRepository.existsByMsgId(msgId)) {
            log.info("Message {} already saved in DB, skipping duplicate", msgId);
            return null;
        }

        final String finalCustomerId = customerId;
        ChatSession session = sessionRepository
                .findFirstByCustomerIdAndStatusOrderByUpdatedAtDesc(finalCustomerId, ChatSessionStatus.OPEN)
                .orElseGet(() -> sessionRepository.findFirstByCustomerIdOrderByUpdatedAtDesc(finalCustomerId)
                        .map(s -> {
                            s.setStatus(ChatSessionStatus.OPEN);
                            return s;
                        })
                        .orElseGet(() -> ChatSession.builder()
                                .customerId(finalCustomerId)
                                .customerName(customerName)
                                .customerPhone(customerPhone)
                                .hotelId(hotelId)
                                .branchName(branchName)
                                .status(ChatSessionStatus.OPEN)
                                .build()));

        if (customerName != null && !customerName.isBlank()) session.setCustomerName(customerName);
        if (customerPhone != null && !customerPhone.isBlank()) session.setCustomerPhone(customerPhone);
        if (hotelId != null) session.setHotelId(hotelId);
        if (branchName != null && !branchName.isBlank()) session.setBranchName(branchName);

        session.setLastMessage(message);
        session.setLastMessageAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        ChatMessage chatMessage = ChatMessage.builder()
                .session(session)
                .senderType(ChatSenderType.CUSTOMER)
                .senderId(finalCustomerId)
                .senderName(customerName != null ? customerName : "Khách hàng")
                .message(message)
                .msgId(msgId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        chatMessage = messageRepository.save(chatMessage);
        log.info("Saved customer chat message to PostgreSQL: id={}, sessionId={}", chatMessage.getId(), session.getId());
        return toMessageResponse(chatMessage);
    }

    @Override
    @Transactional
    public ChatMessageResponse saveStaffMessage(
            String customerId,
            String staffId,
            String staffName,
            Long hotelId,
            String branchName,
            String message,
            String msgId
    ) {
        if (customerId == null || customerId.isBlank()) {
            customerId = "guest_anonymous";
        }

        if (msgId != null && !msgId.isBlank() && messageRepository.existsByMsgId(msgId)) {
            log.info("Staff message {} already saved in DB, skipping duplicate", msgId);
            return null;
        }

        final String finalCustomerId = customerId;
        ChatSession session = sessionRepository
                .findFirstByCustomerIdOrderByUpdatedAtDesc(finalCustomerId)
                .orElseGet(() -> ChatSession.builder()
                        .customerId(finalCustomerId)
                        .customerName("Khách hàng")
                        .hotelId(hotelId)
                        .branchName(branchName)
                        .status(ChatSessionStatus.OPEN)
                        .build());

        session.setLastMessage(message);
        session.setLastMessageAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        ChatMessage chatMessage = ChatMessage.builder()
                .session(session)
                .senderType(ChatSenderType.STAFF)
                .senderId(staffId)
                .senderName(staffName != null ? staffName : "Nhân viên Sen Việt")
                .message(message)
                .msgId(msgId)
                .isRead(true)
                .createdAt(LocalDateTime.now())
                .build();

        chatMessage = messageRepository.save(chatMessage);
        log.info("Saved staff chat message to PostgreSQL: id={}, sessionId={}", chatMessage.getId(), session.getId());
        return toMessageResponse(chatMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesByCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) return Collections.emptyList();

        return sessionRepository.findFirstByCustomerIdOrderByUpdatedAtDesc(customerId)
                .map(session -> messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                        .stream()
                        .map(this::toMessageResponse)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getAllSessions(Long hotelId) {
        List<ChatSession> sessions = (hotelId != null)
                ? sessionRepository.findAllByHotelIdOrderByUpdatedAtDesc(hotelId)
                : sessionRepository.findAllByOrderByUpdatedAtDesc();

        return sessions.stream().map(s -> {
            List<ChatMessageResponse> msgs = messageRepository.findBySessionIdOrderByCreatedAtAsc(s.getId())
                    .stream()
                    .map(this::toMessageResponse)
                    .collect(Collectors.toList());

            return ChatSessionResponse.builder()
                    .id(s.getId())
                    .customerId(s.getCustomerId())
                    .customerName(s.getCustomerName())
                    .customerPhone(s.getCustomerPhone())
                    .hotelId(s.getHotelId())
                    .branchName(s.getBranchName())
                    .status(s.getStatus())
                    .lastMessage(s.getLastMessage())
                    .lastMessageAt(s.getLastMessageAt())
                    .createdAt(s.getCreatedAt())
                    .messages(msgs)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void closeSession(Long sessionId) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setStatus(ChatSessionStatus.CLOSED);
            sessionRepository.save(s);
        });
    }

    private ChatMessageResponse toMessageResponse(ChatMessage m) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .sessionId(m.getSession() != null ? m.getSession().getId() : null)
                .senderType(m.getSenderType())
                .senderId(m.getSenderId())
                .senderName(m.getSenderName())
                .message(m.getMessage())
                .msgId(m.getMsgId())
                .isRead(m.getIsRead())
                .createdAt(m.getCreatedAt())
                .build();
    }
}

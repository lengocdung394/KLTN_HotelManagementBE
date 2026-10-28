package iuh.fit.se.hotelmanagement_be.modular.chat.repositories;

import iuh.fit.se.hotelmanagement_be.modular.chat.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    boolean existsByMsgId(String msgId);
}

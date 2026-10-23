package iuh.fit.se.hotelmanagement_be.modular.chat.repositories;

import iuh.fit.se.hotelmanagement_be.modular.chat.entities.ChatSession;
import iuh.fit.se.hotelmanagement_be.modular.chat.enums.ChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findFirstByCustomerIdAndStatusOrderByUpdatedAtDesc(String customerId, ChatSessionStatus status);
    Optional<ChatSession> findFirstByCustomerIdOrderByUpdatedAtDesc(String customerId);
    List<ChatSession> findAllByOrderByUpdatedAtDesc();
    List<ChatSession> findAllByHotelIdOrderByUpdatedAtDesc(Long hotelId);
}

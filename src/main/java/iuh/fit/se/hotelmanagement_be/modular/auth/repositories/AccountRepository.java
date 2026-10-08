package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    // Fetch sẵn cả User, Hotel và Roles đi kèm để tránh lỗi Lazy loading khi kiểm tra quyền
    @Query("SELECT DISTINCT a FROM Account a " +
            "LEFT JOIN FETCH a.employee e " +
            "LEFT JOIN FETCH e.hotel " +
            "LEFT JOIN FETCH a.customer c " +
            "LEFT JOIN FETCH a.roles " +
            "WHERE a.email = :email")
    Optional<Account> findByEmail(@Param("email") String email);

    boolean existsByEmail(String email);


    // Phan func cho phan super admin

    boolean existsByEmailAndIdNot(String email, String id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, String id);

    @Query("SELECT DISTINCT a FROM Account a " +
            "LEFT JOIN FETCH a.employee e " +
            "LEFT JOIN FETCH e.hotel " +
            "LEFT JOIN FETCH a.customer c " +
            "LEFT JOIN FETCH a.roles")
    List<Account> findAllWithProfiles();

    @Query("SELECT DISTINCT a FROM Account a " +
            "LEFT JOIN FETCH a.employee e " +
            "LEFT JOIN FETCH e.hotel " +
            "LEFT JOIN FETCH a.customer c " +
            "LEFT JOIN FETCH a.roles " +
            "WHERE a.id = :id")
    Optional<Account> findByIdWithProfiles(@Param("id") String id);

}

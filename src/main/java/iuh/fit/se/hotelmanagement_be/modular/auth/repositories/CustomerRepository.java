package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByPhone(String phone);

    boolean existsByCccd(String cccd);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByAccount(iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account account);
}

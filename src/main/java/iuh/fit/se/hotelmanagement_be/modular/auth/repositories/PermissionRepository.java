package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
    @Query("SELECT r FROM Permission r")
    List<Permission> findAll();

    Page<Permission> findAll(Pageable pageable);

    Page<Permission> findByNameContaining(
            String fullNamePattern,
            Pageable pageable
    );

}

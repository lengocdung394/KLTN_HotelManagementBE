package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleRepository  extends JpaRepository<Role,Long> {
    Optional<Role> findByName(String name);
    @Query("SELECT r FROM Role r WHERE r.name <> 'ROLE_CUSTOMER'")
    List<Role> findAllExceptCustomer();

    @Query("SELECT r FROM Role r")
    List<Role> findAll();


    Page<Role> findAll(Pageable pageable);

    Page<Role> findByNameContaining(
            String fullNamePattern,
            Pageable pageable
    );

    boolean existsByPermissions_Id(Long permissionId);
}

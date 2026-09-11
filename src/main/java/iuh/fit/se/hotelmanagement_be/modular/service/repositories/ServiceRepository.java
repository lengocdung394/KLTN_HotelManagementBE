package iuh.fit.se.hotelmanagement_be.modular.service.repositories;

import iuh.fit.se.hotelmanagement_be.modular.service.entities.Service;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<Service,Long> {
}

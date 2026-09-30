package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    java.util.List<Employee> findByHotelId(Long hotelId);
    boolean existsByPhone(String phone);
    boolean existsByCccd(String cccd);
    boolean existsByPhoneAndIdNot(String phone, String id);
    boolean existsByCccdAndIdNot(String cccd, String id);
}

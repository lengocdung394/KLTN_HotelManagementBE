package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface BookingServiceDetailRepository extends JpaRepository<BookingServiceDetail, Long> {
    // Có thể tìm danh sách dịch vụ theo bookingDetailId nếu cần
    List<BookingServiceDetail> findByBookingDetailId(Long bookingDetailId);
}

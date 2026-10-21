package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.HousekeepingTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, String> {
    List<HousekeepingTask> findByAssignedEmployee_IdAndStatusInOrderByCreatedAtAsc(
            String employeeId,
            List<String> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT task FROM HousekeepingTask task WHERE task.status = 'ASSIGNED' AND task.assignedAt <= :cutoff")
    List<HousekeepingTask> findTasksAwaitingReassignment(@Param("cutoff") LocalDateTime cutoff);
}

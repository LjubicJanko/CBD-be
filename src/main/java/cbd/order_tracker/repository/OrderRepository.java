package cbd.order_tracker.repository;

import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderRecord, Long> {
	@Query("SELECT o FROM OrderRecord o WHERE o.trackingId = :trackingId AND o.deleted = false")
	Optional<OrderRecord> findByTrackingId(String trackingId);

	@Query("SELECT o FROM OrderRecord o WHERE o.status IN :statuses AND o.deleted = false")
	List<OrderRecord> findByStatusIn(List<OrderStatus> statuses);

	@Query("SELECT o FROM OrderRecord o WHERE (o.name LIKE %:nameTerm% OR o.description LIKE %:descriptionTerm%) AND o.deleted = false")
	Page<OrderRecord> findByNameContainingOrDescriptionContaining(String nameTerm, String descriptionTerm, Pageable pageable);

	@Query("SELECT o FROM OrderRecord o WHERE o.status IN :statuses AND o.deleted = false")
	Page<OrderRecord> findAllByStatusIn(List<OrderStatus> statuses, Pageable pageable);

	@Query("SELECT o FROM OrderRecord o WHERE o.executionStatus IN :executionStatuses AND o.deleted = false")
	Page<OrderRecord> findAllByExecutionStatusIn(List<OrderExecutionStatus> executionStatuses, Pageable pageable);

	@Query("SELECT o FROM OrderRecord o WHERE o.status IN :statuses AND o.executionStatus IN :executionStatuses AND o.deleted = false")
	Page<OrderRecord> findAllByStatusInAndExecutionStatusIn(
			List<OrderStatus> statuses,
			List<OrderExecutionStatus> executionStatuses,
			Pageable pageable);
}

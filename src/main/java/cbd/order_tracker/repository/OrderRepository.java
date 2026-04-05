package cbd.order_tracker.repository;

import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderPriority;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.dto.OrderOverviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

	@Query("SELECT new cbd.order_tracker.model.dto.OrderOverviewDto(" +
			"o.id, o.name, o.description, o.plannedEndingDate, o.status, o.priority, o.executionStatus, " +
			"o.dateWhenMovedToDone, o.postalCode, o.postalService, o.salePrice, o.salePriceWithTax, " +
			"o.legalEntity, o.amountPaid) " +
			"FROM OrderRecord o " +
			"WHERE (:searchTerm IS NULL OR o.name LIKE %:searchTerm% OR o.description LIKE %:searchTerm%) AND " +
			"(:statuses IS NULL OR o.status IN :statuses) AND " +
			"(:priorities IS NULL OR o.priority IN :priorities) AND " +
			"(:executionStatuses IS NULL OR o.executionStatus IN :executionStatuses) AND " +
			"o.deleted = false")
	Page<OrderOverviewDto> findOverviewBySearchAndFilters(
			@Param("searchTerm") String searchTerm,
			@Param("statuses") List<OrderStatus> statuses,
			@Param("priorities") List<OrderPriority> priorities,
			@Param("executionStatuses") List<OrderExecutionStatus> executionStatuses,
			Pageable pageable
	);

	@Query("SELECT o FROM OrderRecord o WHERE o.status = cbd.order_tracker.model.OrderStatus.DONE " +
			"AND o.executionStatus <> cbd.order_tracker.model.OrderExecutionStatus.CANCELED " +
			"AND (:from IS NULL OR o.creationTime >= :from) " +
			"AND (:to IS NULL OR o.creationTime <= :to)")
	List<OrderRecord> findCompletedOrders(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	@Query("SELECT COUNT(o), " +
			"COALESCE(SUM(o.acquisitionCost), 0), " +
			"COALESCE(AVG(o.acquisitionCost), 0), " +
			"COALESCE(SUM(o.amountPaid), 0), " +
			"COALESCE(SUM(o.salePrice), 0), " +
			"COALESCE(SUM(o.amountLeftToPay), 0), " +
			"SUM(CASE WHEN o.extension = true THEN 1 ELSE 0 END), " +
			"SUM(CASE WHEN o.extension = false THEN 1 ELSE 0 END) " +
			"FROM OrderRecord o " +
			"WHERE o.executionStatus <> cbd.order_tracker.model.OrderExecutionStatus.CANCELED " +
			"AND (:from IS NULL OR o.creationTime >= :from) " +
			"AND (:to IS NULL OR o.creationTime <= :to)")
	Object[] getOrderReport(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

}

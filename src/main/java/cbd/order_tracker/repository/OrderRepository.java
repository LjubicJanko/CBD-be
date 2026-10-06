package cbd.order_tracker.repository;

import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderPriority;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.dto.OrderOverviewDto;
import cbd.order_tracker.model.dto.response.UnpaidOrderRowDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderRecord, Long> {
	@Query("SELECT o FROM OrderRecord o WHERE o.trackingId = :trackingId AND o.deleted = false AND o.tenant.id = :tenantId")
	Optional<OrderRecord> findByTrackingId(@Param("trackingId") String trackingId, @Param("tenantId") Long tenantId);

	@Query("SELECT o FROM OrderRecord o JOIN o.aliasIds a WHERE a = :aliasId AND o.deleted = false AND o.tenant.id = :tenantId")
	Optional<OrderRecord> findByAliasId(@Param("aliasId") String aliasId, @Param("tenantId") Long tenantId);

	@Query("SELECT o FROM OrderRecord o WHERE o.status IN :statuses AND o.deleted = false AND o.tenant.id = :tenantId")
	List<OrderRecord> findByStatusIn(@Param("statuses") List<OrderStatus> statuses, @Param("tenantId") Long tenantId);

	@Query("SELECT o FROM OrderRecord o WHERE o.deleted = false AND o.tenant.id = :tenantId")
	List<OrderRecord> findAllByTenant(@Param("tenantId") Long tenantId);

	@Query("SELECT o FROM OrderRecord o WHERE o.deleted = false AND o.tenant.id = :tenantId")
	Page<OrderRecord> findAllByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

	@Query("SELECT o FROM OrderRecord o WHERE (o.name LIKE %:nameTerm% OR o.description LIKE %:descriptionTerm%) AND o.deleted = false AND o.tenant.id = :tenantId")
	Page<OrderRecord> findByNameContainingOrDescriptionContaining(@Param("nameTerm") String nameTerm, @Param("descriptionTerm") String descriptionTerm, @Param("tenantId") Long tenantId, Pageable pageable);

	@Query("SELECT new cbd.order_tracker.model.dto.OrderOverviewDto(" +
			"o.id, o.name, o.description, o.plannedEndingDate, o.status, o.priority, o.executionStatus, " +
			"o.dateWhenMovedToDone, o.postalCode, o.postalService, o.salePrice, o.salePriceWithTax, " +
			"o.legalEntity, o.amountPaid, o.extension, o.printFilesUrl) " +
			"FROM OrderRecord o " +
			"WHERE (:searchTerm IS NULL OR o.name LIKE %:searchTerm% OR o.description LIKE %:searchTerm%) AND " +
			"(:statuses IS NULL OR o.status IN :statuses) AND " +
			"(:priorities IS NULL OR o.priority IN :priorities) AND " +
			"(:executionStatuses IS NULL OR o.executionStatus IN :executionStatuses) AND " +
			"o.deleted = false AND o.tenant.id = :tenantId")
	Page<OrderOverviewDto> findOverviewBySearchAndFilters(
			@Param("searchTerm") String searchTerm,
			@Param("statuses") List<OrderStatus> statuses,
			@Param("priorities") List<OrderPriority> priorities,
			@Param("executionStatuses") List<OrderExecutionStatus> executionStatuses,
			@Param("tenantId") Long tenantId,
			Pageable pageable
	);

	@Query("SELECT o FROM OrderRecord o WHERE o.status = cbd.order_tracker.model.OrderStatus.DONE " +
			"AND o.executionStatus <> cbd.order_tracker.model.OrderExecutionStatus.CANCELED " +
			"AND (:from IS NULL OR o.dateWhenMovedToDone >= :from) " +
			"AND (:to IS NULL OR o.dateWhenMovedToDone <= :to) " +
			"AND o.tenant.id = :tenantId")
	List<OrderRecord> findCompletedOrders(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("tenantId") Long tenantId);

	@Query("SELECT COUNT(o), " +
			"COALESCE(SUM(o.acquisitionCost), 0), " +
			"COALESCE(AVG(o.acquisitionCost), 0), " +
			"COALESCE(SUM(o.amountPaid), 0), " +
			"COALESCE(SUM(o.salePrice), 0), " +
			"COALESCE(SUM(o.amountLeftToPay), 0), " +
			"COALESCE(SUM(CASE WHEN o.extension = true THEN 1 ELSE 0 END), 0), " +
			"COALESCE(SUM(CASE WHEN o.extension = false THEN 1 ELSE 0 END), 0) " +
			"FROM OrderRecord o " +
			"WHERE o.executionStatus <> cbd.order_tracker.model.OrderExecutionStatus.CANCELED " +
			"AND (:from IS NULL OR o.dateWhenMovedToDone >= :from) " +
			"AND (:to IS NULL OR o.dateWhenMovedToDone <= :to) " +
			"AND o.tenant.id = :tenantId")
	Object[] getOrderReport(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("tenantId") Long tenantId);

	// Single source of truth for the unpaid list and the Outstanding card. Never reads the stored amountLeftToPay column.
	String UNPAID_BALANCE = "(CASE WHEN o.legalEntity = true THEN COALESCE(o.salePriceWithTax, 0) ELSE COALESCE(o.salePrice, 0) END " +
			"- COALESCE(o.amountPaid, 0))";
	String UNPAID_BASE_WHERE = "WHERE o.deleted = false AND o.tenant.id = :tenantId " +
			"AND o.executionStatus <> cbd.order_tracker.model.OrderExecutionStatus.CANCELED " +
			"AND (:from IS NULL OR o.creationTime >= :from) " +
			"AND (:to IS NULL OR o.creationTime <= :to) " +
			"AND " + UNPAID_BALANCE + " > 0";
	String UNPAID_NO_PAYMENTS_IN_RANGE = " AND (:withoutPaymentsInRange = false OR NOT EXISTS (" +
			"SELECT 1 FROM Payment px WHERE px.order = o " +
			"AND (:paymentFrom IS NULL OR px.paymentDate >= :paymentFrom) " +
			"AND (:paymentTo IS NULL OR px.paymentDate <= :paymentTo)))";
	String UNPAID_SELECT = "SELECT new cbd.order_tracker.model.dto.response.UnpaidOrderRowDto(" +
			"o.id, o.name, o.trackingId, o.status, o.executionStatus, o.salePrice, o.amountPaid, " +
			UNPAID_BALANCE + ", " +
			"(SELECT MAX(pl.paymentDate) FROM Payment pl WHERE pl.order = o), " +
			"(SELECT COUNT(pc) FROM Payment pc WHERE pc.order = o)) " +
			"FROM OrderRecord o ";
	String UNPAID_COUNT = "SELECT COUNT(o) FROM OrderRecord o ";

	@Query(value = UNPAID_SELECT + UNPAID_BASE_WHERE + UNPAID_NO_PAYMENTS_IN_RANGE +
			" ORDER BY " + UNPAID_BALANCE + " DESC, o.id DESC",
			countQuery = UNPAID_COUNT + UNPAID_BASE_WHERE + UNPAID_NO_PAYMENTS_IN_RANGE)
	Page<UnpaidOrderRowDto> findUnpaidOrdersDesc(
			@Param("tenantId") Long tenantId,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to,
			@Param("withoutPaymentsInRange") boolean withoutPaymentsInRange,
			@Param("paymentFrom") LocalDate paymentFrom,
			@Param("paymentTo") LocalDate paymentTo,
			Pageable pageable);

	@Query(value = UNPAID_SELECT + UNPAID_BASE_WHERE + UNPAID_NO_PAYMENTS_IN_RANGE +
			" ORDER BY " + UNPAID_BALANCE + " ASC, o.id ASC",
			countQuery = UNPAID_COUNT + UNPAID_BASE_WHERE + UNPAID_NO_PAYMENTS_IN_RANGE)
	Page<UnpaidOrderRowDto> findUnpaidOrdersAsc(
			@Param("tenantId") Long tenantId,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to,
			@Param("withoutPaymentsInRange") boolean withoutPaymentsInRange,
			@Param("paymentFrom") LocalDate paymentFrom,
			@Param("paymentTo") LocalDate paymentTo,
			Pageable pageable);

	/** Unrounded sum of the unpaid balances over the whole filtered set (null when empty). */
	@Query("SELECT SUM(" + UNPAID_BALANCE + ") FROM OrderRecord o " + UNPAID_BASE_WHERE + UNPAID_NO_PAYMENTS_IN_RANGE)
	BigDecimal sumUnpaidBalance(
			@Param("tenantId") Long tenantId,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to,
			@Param("withoutPaymentsInRange") boolean withoutPaymentsInRange,
			@Param("paymentFrom") LocalDate paymentFrom,
			@Param("paymentTo") LocalDate paymentTo);

	/**
	 * Outstanding card total: same WHERE and balance expression as the unpaid list (creation-date range).
	 * Rows with a balance <= 0 are excluded by the shared WHERE, which is equivalent to
	 * SUM(CASE WHEN balance > 0 THEN balance ELSE 0 END) but avoids mixed-type CASE literals. Null when empty.
	 */
	@Query("SELECT SUM(" + UNPAID_BALANCE + ") FROM OrderRecord o " + UNPAID_BASE_WHERE)
	BigDecimal getOutstandingTotal(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("tenantId") Long tenantId);

}

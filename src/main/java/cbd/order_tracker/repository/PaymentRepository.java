package cbd.order_tracker.repository;

import cbd.order_tracker.model.Payment;
import cbd.order_tracker.model.PaymentMethod;
import cbd.order_tracker.model.dto.response.PaymentReportRowDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

	// Shared by every report query. The order join is explicit and o.deleted = false is explicit because
	// @SQLRestriction on OrderRecord is not relied on for the Payment -> order path.
	String REPORT_BASE_FROM = "FROM Payment p JOIN p.order o ";
	String REPORT_BASE_WHERE = "WHERE o.deleted = false AND o.tenant.id = :tenantId " +
			"AND (:from IS NULL OR p.paymentDate >= :from) " +
			"AND (:to IS NULL OR p.paymentDate <= :to)";
	String METHODS_FILTER = " AND (:methodFilter = false " +
			"OR (:hasMethods = true AND p.paymentMethod IN :methods) " +
			"OR (:includeUnspecified = true AND p.paymentMethod IS NULL))";

	void delete(Payment paymentToDelete);

	List<Payment> findByOrderId(Long orderId);

	/** Paged payment rows as a DTO projection (Payment entities are never loaded). Sort comes from the Pageable. */
	@Query(value = "SELECT new cbd.order_tracker.model.dto.response.PaymentReportRowDto(" +
			"p.id, o.id, o.name, o.trackingId, p.payer, p.amount, p.paymentDate, p.paymentMethod, p.note) " +
			REPORT_BASE_FROM + REPORT_BASE_WHERE + METHODS_FILTER,
			countQuery = "SELECT COUNT(p) " + REPORT_BASE_FROM + REPORT_BASE_WHERE + METHODS_FILTER)
	Page<PaymentReportRowDto> findPaymentReportRows(
			@Param("tenantId") Long tenantId,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to,
			@Param("methodFilter") boolean methodFilter,
			@Param("hasMethods") boolean hasMethods,
			@Param("methods") List<PaymentMethod> methods,
			@Param("includeUnspecified") boolean includeUnspecified,
			Pageable pageable);

	/** One row per payment method (null method = its own group): [PaymentMethod|null, BigDecimal sum, Long count]. Ignores the methods filter. */
	@Query("SELECT p.paymentMethod, SUM(p.amount), COUNT(p) " + REPORT_BASE_FROM + REPORT_BASE_WHERE +
			" GROUP BY p.paymentMethod")
	List<Object[]> sumPaymentsByMethod(
			@Param("tenantId") Long tenantId,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to);

	/** Sum of all payments by payment date (no method or order status filter). Null when there are none. */
	@Query("SELECT SUM(p.amount) " + REPORT_BASE_FROM + REPORT_BASE_WHERE)
	BigDecimal sumPaymentAmounts(
			@Param("tenantId") Long tenantId,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to);
}

package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.exceptions.GlobalExceptionHandler;
import cbd.order_tracker.exceptions.OrderNotFoundException;
import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.OrderStatusHistory;
import cbd.order_tracker.model.Privilege;
import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.OrderStatusHistoryDTO;
import cbd.order_tracker.repository.OrderRepository;
import cbd.order_tracker.repository.OrderStatusHistoryRepository;
import cbd.order_tracker.repository.PaymentRepository;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.util.UserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static cbd.order_tracker.model.OrderExecutionStatus.ACTIVE;
import static cbd.order_tracker.model.OrderExecutionStatus.ARCHIVED;
import static cbd.order_tracker.model.OrderExecutionStatus.CANCELED;
import static cbd.order_tracker.model.OrderExecutionStatus.PAUSED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplExecutionStatusTest {

	private static final Long TENANT_ID = 1L;
	private static final Long ORDER_ID = 100L;
	private static final String DENIED = "Only a company admin can change the status of an archived or canceled order";

	private OrderRepository orderRepository;
	private UserUtil userUtil;
	private OrderStatusHistoryRepository historyRepository;
	private OrderServiceImpl service;
	private Tenant tenant;
	private OrderRecord order;

	@BeforeEach
	void setup() {
		orderRepository = mock(OrderRepository.class);
		userUtil = mock(UserUtil.class);
		historyRepository = mock(OrderStatusHistoryRepository.class);
		service = new OrderServiceImpl(userUtil, orderRepository, mock(PaymentRepository.class),
				historyRepository, mock(TenantRepository.class));
		tenant = new Tenant("Acme", "acme");
		tenant.setId(TENANT_ID);
		TenantContext.setSuperadmin(false);
		TenantContext.setTenantId(TENANT_ID);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("tester", "pw", List.of()));
		when(orderRepository.save(any(OrderRecord.class))).thenAnswer(i -> i.getArgument(0));
		// background: order 100 with workflow status SEWING and execution status ACTIVE
		order = storedOrder(ORDER_ID, tenant, ACTIVE);
	}

	@AfterEach
	void cleanup() {
		TenantContext.clear();
		SecurityContextHolder.clearContext();
	}

	private OrderRecord storedOrder(Long id, Tenant orderTenant, OrderExecutionStatus execution) {
		OrderRecord incoming = new OrderRecord();
		incoming.setName("A");
		incoming.setSalePrice(new BigDecimal("100"));
		incoming.setAcquisitionCost(new BigDecimal("40"));
		OrderRecord r = new OrderRecord(incoming);
		r.setId(id);
		r.setTenant(orderTenant);
		r.setStatus(OrderStatus.SEWING);
		r.setExecutionStatus(execution);
		r.setPausingComment("old reason");
		r.setStatusHistory(new ArrayList<>()); // drop the constructor's initial workflow row; tests seed their own
		when(orderRepository.findById(id)).thenReturn(Optional.of(r));
		when(historyRepository.findByOrderId(id)).thenAnswer(i -> r.getStatusHistory());
		return r;
	}

	private void orderIs(OrderExecutionStatus execution) {
		order.setExecutionStatus(execution);
	}

	private void role(String roleName) {
		when(userUtil.getCurrentUserRoles()).thenReturn(Set.of(new Role(roleName)));
	}

	private void roleWithPrivilege(String roleName, String privilege) {
		Role role = new Role(roleName);
		role.setPrivileges(Set.of(new Privilege(privilege)));
		when(userUtil.getCurrentUserRoles()).thenReturn(Set.of(role));
	}

	private void superadminWithTenant() {
		TenantContext.setSuperadmin(true);
		TenantContext.setTenantId(TENANT_ID);
		role("manufacturer");
	}

	private void authenticatedAs(String fullName) {
		User user = new User();
		user.setFullName(fullName);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(user, "pw", List.of()));
	}

	private OrderStatusHistory executionRow(Long id, OrderExecutionStatus status, LocalDateTime time) {
		OrderStatusHistory h = new OrderStatusHistory(order, status, null);
		h.setId(id);
		h.setCreationTime(time);
		order.getStatusHistory().add(h);
		return h;
	}

	private void assertDeniedAndUnchanged(Runnable action, OrderExecutionStatus expected) {
		assertThatThrownBy(action::run)
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage(DENIED);
		assertThat(order.getExecutionStatus()).isEqualTo(expected);
		assertThat(order.getPausingComment()).isEqualTo("old reason");
		assertThat(order.getStatusHistory()).isEmpty();
		verify(orderRepository, never()).save(any(OrderRecord.class));
	}

	// --- Scenario: Company admin unarchives an order and workflow status is preserved
	@Test
	void companyAdminUnarchivesOrderAndWorkflowStatusIsPreserved() {
		orderIs(ARCHIVED);
		role("company_admin");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ACTIVE, "");

		assertThat(dto.getExecutionStatus()).isEqualTo(ACTIVE);
		assertThat(dto.getStatus()).isEqualTo(OrderStatus.SEWING);
	}

	// --- Scenario: Superadmin with tenant context unarchives an order
	@Test
	void superadminWithTenantContextUnarchivesOrder() {
		orderIs(ARCHIVED);
		superadminWithTenant();

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ACTIVE, "");

		assertThat(dto.getExecutionStatus()).isEqualTo(ACTIVE);
		assertThat(dto.getStatus()).isEqualTo(OrderStatus.SEWING);
	}

	// --- Scenario: Company admin reactivates a canceled order and workflow status is preserved
	@Test
	void companyAdminReactivatesCanceledOrderAndWorkflowStatusIsPreserved() {
		orderIs(CANCELED);
		role("company_admin");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ACTIVE, "");

		assertThat(dto.getExecutionStatus()).isEqualTo(ACTIVE);
		assertThat(dto.getStatus()).isEqualTo(OrderStatus.SEWING);
	}

	// --- Scenario: Superadmin with tenant context reactivates a canceled order
	@Test
	void superadminWithTenantContextReactivatesCanceledOrder() {
		orderIs(CANCELED);
		superadminWithTenant();

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ACTIVE, "");

		assertThat(dto.getExecutionStatus()).isEqualTo(ACTIVE);
	}

	// --- Scenario: Admin moves an archived order to a non-ACTIVE status
	@Test
	void adminMovesArchivedOrderToPaused() {
		orderIs(ARCHIVED);
		role("company_admin");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, PAUSED, "hold");

		assertThat(dto.getExecutionStatus()).isEqualTo(PAUSED);
	}

	// --- Scenario: Admin moves a canceled order to ARCHIVED
	@Test
	void adminMovesCanceledOrderToArchived() {
		orderIs(CANCELED);
		role("company_admin");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ARCHIVED, "");

		assertThat(dto.getExecutionStatus()).isEqualTo(ARCHIVED);
	}

	// --- Scenario: Admin archives an active order
	@Test
	void adminArchivesActiveOrder() {
		role("company_admin");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ARCHIVED, "");

		assertThat(dto.getExecutionStatus()).isEqualTo(ARCHIVED);
	}

	// --- Scenario Outline: Non-admin cannot unarchive or reactivate
	@ParameterizedTest
	@CsvSource({"ARCHIVED,manager", "ARCHIVED,manufacturer", "CANCELED,manager", "CANCELED,manufacturer"})
	void nonAdminCannotUnarchiveOrReactivate(OrderExecutionStatus current, String roleName) {
		orderIs(current);
		role(roleName);

		assertDeniedAndUnchanged(() -> service.changeExecutionStatus(ORDER_ID, ACTIVE, ""), current);

		// the 403 body produced by the existing handler
		ProblemDetail pd = new GlobalExceptionHandler().handleSecurityException(new AccessDeniedException(DENIED));
		assertThat(pd.getStatus()).isEqualTo(403);
		assertThat(pd.getDetail()).isEqualTo(DENIED);
		assertThat(pd.getProperties().get("description")).isEqualTo("You are not authorized to access this resource");
	}

	// --- Scenario Outline: Non-admin cannot move a canceled or archived order to another non-ACTIVE status
	@ParameterizedTest
	@CsvSource({"ARCHIVED,PAUSED", "ARCHIVED,CANCELED", "CANCELED,PAUSED", "CANCELED,ARCHIVED"})
	void nonAdminCannotMoveCanceledOrArchivedOrderToAnotherNonActiveStatus(OrderExecutionStatus current,
			OrderExecutionStatus target) {
		orderIs(current);
		role("manager");

		assertDeniedAndUnchanged(() -> service.changeExecutionStatus(ORDER_ID, target, ""), current);
	}

	// --- Scenario: Non-admin cannot archive an order
	@Test
	void nonAdminCannotArchiveOrder() {
		role("manager");

		assertDeniedAndUnchanged(() -> service.changeExecutionStatus(ORDER_ID, ARCHIVED, ""), ACTIVE);
	}

	// --- Scenario Outline: Non-admin cannot bypass via /reactivate
	@ParameterizedTest
	@EnumSource(value = OrderExecutionStatus.class, names = {"ARCHIVED", "CANCELED"})
	void nonAdminCannotBypassViaReactivate(OrderExecutionStatus current) {
		orderIs(current);
		role("manufacturer");

		assertDeniedAndUnchanged(() -> service.reactivateOrder(ORDER_ID), current);
	}

	// --- Scenario Outline: Non-admin cannot bypass via /pause
	@ParameterizedTest
	@EnumSource(value = OrderExecutionStatus.class, names = {"ARCHIVED", "CANCELED"})
	void nonAdminCannotBypassViaPause(OrderExecutionStatus current) {
		orderIs(current);
		role("manager");

		assertDeniedAndUnchanged(() -> service.pauseOrder(ORDER_ID, "comment"), current);
	}

	// --- Scenario: Admin can reactivate an archived order through /reactivate
	@Test
	void adminCanReactivateArchivedOrderThroughReactivate() {
		orderIs(ARCHIVED);
		role("company_admin");

		OrderDTO dto = service.reactivateOrder(ORDER_ID);

		assertThat(dto.getExecutionStatus()).isEqualTo(ACTIVE);
		List<OrderStatusHistoryDTO> history = dto.getStatusHistory();
		assertThat(history.get(history.size() - 1).getClosingComment()).isNull();
	}

	// --- Scenario: Canceling requires the order-cancel privilege or admin
	@Test
	void nonAdminWithOrderCancelPrivilegeCanCancelOrder() {
		roleWithPrivilege("manager", "order-cancel");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, CANCELED, "customer left");

		assertThat(dto.getExecutionStatus()).isEqualTo(CANCELED);
	}

	@Test
	void companyAdminCanCancelOrderWithoutExplicitPrivilege() {
		role("company_admin");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, CANCELED, "customer left");

		assertThat(dto.getExecutionStatus()).isEqualTo(CANCELED);
	}

	@ParameterizedTest
	@CsvSource({"ACTIVE", "PAUSED"})
	void nonAdminWithoutOrderCancelPrivilegeCannotCancelOrder(OrderExecutionStatus current) {
		orderIs(current);
		roleWithPrivilege("manager", "order-pause");

		assertThatThrownBy(() -> service.changeExecutionStatus(ORDER_ID, CANCELED, "oops"))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("The order-cancel privilege is required to cancel an order");
		assertThat(order.getExecutionStatus()).isEqualTo(current);
		assertThat(order.getPausingComment()).isEqualTo("old reason");
		assertThat(order.getStatusHistory()).isEmpty();
		verify(orderRepository, never()).save(any(OrderRecord.class));
	}

	// --- Scenario: ACTIVE to PAUSED and back stays open to non-admins
	@Test
	void activeToPausedAndBackStaysOpenToNonAdmins() {
		role("manufacturer");

		OrderDTO first = service.changeExecutionStatus(ORDER_ID, PAUSED, "wait");
		OrderDTO second = service.changeExecutionStatus(ORDER_ID, ACTIVE, "");

		assertThat(first.getExecutionStatus()).isEqualTo(PAUSED);
		assertThat(second.getExecutionStatus()).isEqualTo(ACTIVE);
	}

	// --- Scenario: Same-status call on an archived or canceled order requires admin
	@ParameterizedTest
	@CsvSource({"ARCHIVED", "CANCELED"})
	void sameStatusCallOnArchivedOrCanceledOrderRequiresAdmin(OrderExecutionStatus current) {
		orderIs(current);
		role("manager");

		assertDeniedAndUnchanged(() -> service.changeExecutionStatus(ORDER_ID, current, "new reason"), current);
	}

	@Test
	void sameStatusCallOnActiveOrderStaysOpenToNonAdmins() {
		role("manufacturer");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ACTIVE, "");

		assertThat(dto.getExecutionStatus()).isEqualTo(ACTIVE);
	}

	// --- Scenario: Order of another tenant returns 404 before any role check
	// NOTE: in the current code TenantGuard throws AccessDeniedException("Cross-tenant access denied") (403),
	// not a 404. What this test pins is the spec's actual invariant: the tenant lookup runs first, so the
	// role-based denial message is never produced and nothing is modified.
	@Test
	void orderOfAnotherTenantIsRejectedByTenantLookupBeforeAnyRoleCheck() {
		Tenant other = new Tenant("Other", "other");
		other.setId(2L);
		OrderRecord foreign = storedOrder(200L, other, ARCHIVED);
		role("manager");

		assertThatThrownBy(() -> service.changeExecutionStatus(200L, ACTIVE, ""))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("Cross-tenant access denied");
		assertThat(foreign.getExecutionStatus()).isEqualTo(ARCHIVED);
		assertThat(foreign.getStatusHistory()).isEmpty();
		verify(userUtil, never()).getCurrentUserRoles();
	}

	// --- Scenario: Missing order returns 404
	@Test
	void missingOrderReturnsNotFound() {
		role("company_admin");
		when(orderRepository.findById(99999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.changeExecutionStatus(99999L, ACTIVE, ""))
				.isInstanceOf(OrderNotFoundException.class);
	}

	// --- Scenario: Changelog entry for admin unarchive is attributable
	@Test
	void changelogEntryForAdminUnarchiveIsAttributable() {
		orderIs(ARCHIVED);
		executionRow(1L, ARCHIVED, LocalDateTime.now().minusDays(1));
		role("company_admin");
		authenticatedAs("Ana Admin");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ACTIVE, "customer resumed");

		assertThat(order.getStatusHistory()).hasSize(2);
		OrderStatusHistory row = order.getStatusHistory().get(1);
		assertThat(row.getExecutionStatus()).isEqualTo(ACTIVE);
		assertThat(row.getUser()).isEqualTo("Ana Admin");
		assertThat(row.getClosingComment()).isEqualTo("customer resumed");
		assertThat(row.getCreationTime()).isNotNull();
		assertThat(row.getStatus()).isNull();
		assertThat(row.getEventType()).isNull();
		OrderStatusHistoryDTO entry = dto.getStatusHistory().get(1);
		assertThat(entry.getExecutionStatus()).isEqualTo(ACTIVE);
		assertThat(entry.getPreviousExecutionStatus()).isEqualTo(ARCHIVED);
	}

	// --- Scenario: Changelog entry for admin reactivation of a canceled order is attributable
	@Test
	void changelogEntryForAdminReactivationOfCanceledOrderIsAttributable() {
		orderIs(CANCELED);
		executionRow(1L, CANCELED, LocalDateTime.now().minusDays(1));
		role("company_admin");
		authenticatedAs("Ana Admin");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ACTIVE, "");

		assertThat(order.getStatusHistory()).hasSize(2);
		OrderStatusHistory row = order.getStatusHistory().get(1);
		assertThat(row.getExecutionStatus()).isEqualTo(ACTIVE);
		assertThat(row.getUser()).isEqualTo("Ana Admin");
		assertThat(dto.getStatusHistory().get(1).getPreviousExecutionStatus()).isEqualTo(CANCELED);
	}

	// --- Scenario: First-ever execution-status row derives previous status ACTIVE
	@Test
	void firstEverExecutionStatusRowDerivesPreviousStatusActive() {
		role("manager");

		OrderDTO dto = service.pauseOrder(ORDER_ID, "wait");

		List<OrderStatusHistoryDTO> history = dto.getStatusHistory();
		OrderStatusHistoryDTO newest = history.get(history.size() - 1);
		assertThat(newest.getExecutionStatus()).isEqualTo(PAUSED);
		assertThat(newest.getPreviousExecutionStatus()).isEqualTo(ACTIVE);
	}

	// --- Scenario: Previous execution status is derived across several consecutive transitions
	@Test
	void previousExecutionStatusIsDerivedAcrossSeveralConsecutiveTransitions() {
		role("company_admin");

		service.changeExecutionStatus(ORDER_ID, PAUSED, null);
		service.changeExecutionStatus(ORDER_ID, CANCELED, null);
		service.changeExecutionStatus(ORDER_ID, ACTIVE, null);
		service.changeExecutionStatus(ORDER_ID, ARCHIVED, null);
		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ACTIVE, null);

		assertThat(dto.getStatusHistory())
				.extracting(OrderStatusHistoryDTO::getExecutionStatus, OrderStatusHistoryDTO::getPreviousExecutionStatus)
				.containsExactly(
						tuple(PAUSED, ACTIVE),
						tuple(CANCELED, PAUSED),
						tuple(ACTIVE, CANCELED),
						tuple(ARCHIVED, ACTIVE),
						tuple(ACTIVE, ARCHIVED));
	}

	// --- Scenario: Legacy order archived before execution history existed (accepted limitation)
	@Test
	void legacyOrderArchivedBeforeExecutionHistoryExistedDerivesPreviousActive() {
		orderIs(ARCHIVED);
		role("company_admin");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ACTIVE, "");

		OrderStatusHistoryDTO entry = dto.getStatusHistory().get(dto.getStatusHistory().size() - 1);
		assertThat(entry.getExecutionStatus()).isEqualTo(ACTIVE);
		assertThat(entry.getPreviousExecutionStatus()).isEqualTo(ACTIVE);
	}

	// --- Scenario: Unarchive overwrites pausingComment with the note
	@Test
	void unarchiveOverwritesPausingCommentWithTheNote() {
		orderIs(ARCHIVED);
		OrderStatusHistory earlier = executionRow(1L, ARCHIVED, LocalDateTime.now().minusDays(1));
		earlier.setClosingComment("old reason");
		role("company_admin");

		OrderDTO dto = service.changeExecutionStatus(ORDER_ID, ACTIVE, "");

		assertThat(dto.getPausingComment()).isEmpty();
		assertThat(order.getStatusHistory().get(0).getClosingComment()).isEqualTo("old reason");
	}
}

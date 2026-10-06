package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.exceptions.GlobalExceptionHandler;
import cbd.order_tracker.exceptions.OrderNotFoundException;
import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.OrderStatusHistory;
import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.request.EditPrintFilesUrlDto;
import cbd.order_tracker.repository.OrderRepository;
import cbd.order_tracker.repository.OrderStatusHistoryRepository;
import cbd.order_tracker.repository.PaymentRepository;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.util.UserUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Scenarios from docs/specs/printfiles-url-on-print-ready. HTTP status codes are verified at the
 * service level: IllegalArgumentException maps to 400 through {@link GlobalExceptionHandler}.
 */
class OrderServiceImplPrintFilesUrlTest {

	private static final Long TENANT_ID = 1L;
	private static final Long ORDER_ID = 10L;
	private static final String LINK = "https://drive.google.com/drive/folders/abc?usp=sharing";
	private static final String OLD = "https://example.com/old";
	private static final String NEW = "https://example.com/new";

	private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

	private OrderRepository orderRepository;
	private UserUtil userUtil;
	private OrderStatusHistoryRepository historyRepository;
	private OrderServiceImpl service;
	private Tenant tenant;
	private OrderRecord order;
	private List<OrderStatusHistory> history;

	@BeforeEach
	void setup() {
		orderRepository = mock(OrderRepository.class);
		userUtil = mock(UserUtil.class);
		historyRepository = mock(OrderStatusHistoryRepository.class);
		TenantRepository tenantRepository = mock(TenantRepository.class);
		service = new OrderServiceImpl(userUtil, orderRepository, mock(PaymentRepository.class),
				historyRepository, tenantRepository);
		tenant = new Tenant("Acme", "acme");
		tenant.setId(TENANT_ID);
		TenantContext.setSuperadmin(false);
		TenantContext.setTenantId(TENANT_ID);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("tester", "pw", List.of()));
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(orderRepository.save(any(OrderRecord.class))).thenAnswer(i -> i.getArgument(0));
		role("company_admin");
	}

	@AfterEach
	void cleanup() {
		TenantContext.clear();
		SecurityContextHolder.clearContext();
	}

	private void role(String roleName) {
		when(userUtil.getCurrentUserRoles()).thenReturn(Set.of(new Role(roleName)));
	}

	private static OrderRecord incoming() {
		OrderRecord o = new OrderRecord();
		o.setId(ORDER_ID);
		o.setName("A");
		o.setSalePrice(new BigDecimal("100"));
		o.setAcquisitionCost(new BigDecimal("40"));
		return o;
	}

	/** Stored order at the given status with one history entry per status reached; link on record and PRINT_READY entry. */
	private OrderRecord orderAt(OrderStatus status, String link) {
		OrderRecord r = new OrderRecord(incoming());
		r.setId(ORDER_ID);
		r.setTenant(tenant);
		r.setStatus(status);
		r.setPrintFilesUrl(link);
		List<OrderStatusHistory> entries = new ArrayList<>();
		OrderStatus first = status == OrderStatus.PENDING ? OrderStatus.PENDING : OrderStatus.DESIGN;
		for (OrderStatus s : OrderStatus.values()) {
			if (s.compareTo(first) >= 0 && s.compareTo(status) <= 0 && !(s == OrderStatus.PENDING && status != s)) {
				OrderStatusHistory h = new OrderStatusHistory(r, s, null, null);
				if (s == OrderStatus.PRINT_READY) {
					h.setPrintFilesUrl(link);
				}
				entries.add(h);
			}
		}
		r.setStatusHistory(entries);
		order = r;
		history = entries;
		when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(r));
		when(historyRepository.findByOrderId(ORDER_ID)).thenReturn(entries);
		return r;
	}

	private OrderStatusHistory printReadyEntry() {
		return history.stream().filter(h -> h.getStatus() == OrderStatus.PRINT_READY).findFirst().orElseThrow();
	}

	private OrderDTO changeStatus(String closingComment, String postalCode, String postalService, String url) {
		return service.changeStatus(ORDER_ID, closingComment, postalCode, postalService, url);
	}

	private static EditPrintFilesUrlDto editDto(String url) {
		EditPrintFilesUrlDto dto = new EditPrintFilesUrlDto();
		dto.setPrintFilesUrl(url);
		return dto;
	}

	private JsonNode json(OrderDTO dto) {
		return mapper.valueToTree(dto);
	}

	private static String linkOfLength(int length) {
		String prefix = "https://example.com/";
		return prefix + "a".repeat(length - prefix.length());
	}

	// --- changeStatus: accepting the link ---

	@Test
	void linkIsStoredOnOrderAndPrintReadyHistoryEntryWhenMovingFromDesign() {
		orderAt(OrderStatus.DESIGN, null);

		OrderDTO dto = changeStatus(null, null, null, LINK);

		assertThat(dto.getStatus()).isEqualTo(OrderStatus.PRINT_READY);
		assertThat(dto.getPrintFilesUrl()).isEqualTo(LINK);
		assertThat(dto.getStatusHistory())
				.filteredOn(h -> h.getStatus() == OrderStatus.PRINT_READY)
				.singleElement()
				.satisfies(h -> assertThat(h.getPrintFilesUrl()).isEqualTo(LINK));
		assertThat(order.getPrintFilesUrl()).isEqualTo(LINK);
	}

	@Test
	void linkIsAcceptedWhenMovingFromPendingToPrintReady() {
		orderAt(OrderStatus.PENDING, null);

		OrderDTO dto = changeStatus(null, null, null, "https://drive.google.com/file/d/xyz/view");

		assertThat(dto.getStatus()).isEqualTo(OrderStatus.PRINT_READY);
		assertThat(dto.getPrintFilesUrl()).isEqualTo("https://drive.google.com/file/d/xyz/view");
	}

	/** Outline rows {}, {"printFilesUrl": null} (both reach the service as null), "" and "   ". */
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   "})
	void missingNullOrBlankLinkIsAllowedOnPrintReadyAndStoredAsNull(String value) {
		orderAt(OrderStatus.DESIGN, null);

		OrderDTO dto = changeStatus(null, null, null, value);

		assertThat(dto.getStatus()).isEqualTo(OrderStatus.PRINT_READY);
		JsonNode node = json(dto);
		assertThat(node.has("printFilesUrl")).isTrue();
		assertThat(node.get("printFilesUrl").isNull()).isTrue();
		assertThat(order.getPrintFilesUrl()).isNull();
	}

	@Test
	void valueIsTrimmedBeforeItIsStored() {
		orderAt(OrderStatus.DESIGN, null);

		OrderDTO dto = changeStatus(null, null, null, "  https://example.com/folder  ");

		assertThat(dto.getPrintFilesUrl()).isEqualTo("https://example.com/folder");
	}

	@Test
	void anyHttpsHostIsAcceptedUnderTheDefaultHostPolicy() {
		orderAt(OrderStatus.DESIGN, null);

		OrderDTO dto = changeStatus(null, null, null, "https://www.dropbox.com/s/abc/files.zip");

		assertThat(dto.getPrintFilesUrl()).isEqualTo("https://www.dropbox.com/s/abc/files.zip");
	}

	@Test
	void uppercaseSchemeAndHostQueryStringAndFragmentAreAccepted() {
		orderAt(OrderStatus.DESIGN, null);

		OrderDTO dto = changeStatus(null, null, null, "HTTPS://WeTransfer.com/downloads/x?a=1#frag");

		assertThat(dto.getStatus()).isEqualTo(OrderStatus.PRINT_READY);
	}

	// --- changeStatus: rejecting invalid values ---

	@ParameterizedTest
	@ValueSource(strings = {
			"http://drive.google.com/drive/folders/a",
			"javascript:alert(1)",
			"data:text/html,hi",
			"ftp://example.com/x",
			"drive.google.com/folder",
			"//drive.google.com/folder",
			"/relative/path",
			"https://user@example.com/x",
			"https://user:pw@example.com/x",
			"https://example.com/a b"
	})
	void invalidLinkOnPrintReadyTransitionReturns400AndChangesNothing(String value) {
		orderAt(OrderStatus.DESIGN, null);
		OrderStatusHistory last = history.get(history.size() - 1);
		last.setClosingComment("old");
		last.setUser("original-user");
		int entries = history.size();

		assertThatThrownBy(() -> changeStatus("ready", null, null, value))
				.isInstanceOf(IllegalArgumentException.class)
				.satisfies(e -> assertProblemDetail((IllegalArgumentException) e));

		assertThat(order.getStatus()).isEqualTo(OrderStatus.DESIGN);
		assertThat(history).hasSize(entries);
		assertThat(last.getClosingComment()).isEqualTo("old");
		assertThat(last.getUser()).isEqualTo("original-user");
	}

	private static void assertProblemDetail(IllegalArgumentException e) {
		ProblemDetail pd = new GlobalExceptionHandler().handleIllegalArgumentException(e);
		assertThat(pd.getStatus()).isEqualTo(400);
		assertThat(pd.getDetail()).isNotBlank();
		assertThat(pd.getProperties()).containsKey("description");
		assertThat(pd.getProperties().get("description").toString()).isNotBlank();
	}

	@Test
	void linkLongerThan2048CharactersReturns400() {
		orderAt(OrderStatus.DESIGN, null);

		assertThatThrownBy(() -> changeStatus(null, null, null, linkOfLength(2049)))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.DESIGN);
	}

	@Test
	void linkOfExactly2048CharactersIsAccepted() {
		orderAt(OrderStatus.DESIGN, null);

		OrderDTO dto = changeStatus(null, null, null, linkOfLength(2048));

		assertThat(dto.getStatus()).isEqualTo(OrderStatus.PRINT_READY);
		assertThat(dto.getPrintFilesUrl()).hasSize(2048);
	}

	@Test
	void linkContainingControlCharactersReturns400() {
		orderAt(OrderStatus.DESIGN, null);

		for (String bad : List.of("https://example.com/a\tb", "https://example.com/a\nb")) {
			assertThatThrownBy(() -> changeStatus(null, null, null, bad))
					.isInstanceOf(IllegalArgumentException.class);
		}
		assertThat(order.getStatus()).isEqualTo(OrderStatus.DESIGN);
	}

	// --- changeStatus: other transitions ---

	@ParameterizedTest
	@EnumSource(value = OrderStatus.class, names = {"PRINT_READY", "PRINTING", "SEWING", "SHIP_READY", "SHIPPED"})
	void nonEmptyLinkOnTransitionNotLandingOnPrintReadyReturns400(OrderStatus from) {
		orderAt(from, from == OrderStatus.PRINT_READY ? OLD : null);
		int entries = history.size();

		assertThatThrownBy(() -> changeStatus(null, null, null, "https://drive.google.com/drive/folders/abc"))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(order.getStatus()).isEqualTo(from);
		assertThat(history).hasSize(entries);
		assertThat(order.getPrintFilesUrl()).isEqualTo(from == OrderStatus.PRINT_READY ? OLD : null);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"  "})
	void absentNullOrBlankLinkOnNonPrintReadyTransitionSucceedsAsBefore(String value) {
		orderAt(OrderStatus.PRINTING, null);

		OrderDTO dto = changeStatus(null, null, null, value);

		assertThat(dto.getStatus()).isEqualTo(OrderStatus.SEWING);
	}

	@Test
	void invalidValueOnNonPrintReadyTransitionIsRejectedBeforeAnyMutation() {
		orderAt(OrderStatus.PRINTING, null);
		OrderStatusHistory last = history.get(history.size() - 1);
		last.setClosingComment("old");

		assertThatThrownBy(() -> changeStatus("new", null, null, "https://example.com/x"))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(last.getClosingComment()).isEqualTo("old");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PRINTING);
	}

	@Test
	void changeStatusIsTransactional() throws NoSuchMethodException {
		var method = OrderServiceImpl.class.getMethod("changeStatus",
				Long.class, String.class, String.class, String.class, String.class);

		assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
	}

	@Test
	void existingLinkIsPreservedAcrossLaterStatusChanges() {
		orderAt(OrderStatus.PRINT_READY, "https://example.com/folder");

		OrderDTO dto = changeStatus(null, null, null, null);

		assertThat(dto.getStatus()).isEqualTo(OrderStatus.PRINTING);
		assertThat(dto.getPrintFilesUrl()).isEqualTo("https://example.com/folder");
	}

	@Test
	void existingPostalBehaviourIsUnchanged() {
		orderAt(OrderStatus.SHIP_READY, null);

		OrderDTO dto = changeStatus(null, "ABC123", "post", null);

		assertThat(dto.getStatus()).isEqualTo(OrderStatus.SHIPPED);
		assertThat(dto.getPostalCode()).isEqualTo("ABC123");
		assertThat(dto.getPostalService()).isEqualTo("post");
	}

	// --- Always-present null key (service-produced OrderDTOs) ---

	@Test
	void orderDtoAlwaysContainsThePrintFilesUrlKeyOnEveryServiceResponse() {
		orderAt(OrderStatus.DESIGN, null);
		List<OrderDTO> responses = new ArrayList<>();

		responses.add(service.getOrderById(ORDER_ID));
		responses.add(service.updateOrder(incoming()));
		responses.add(service.createOrder(incoming()));
		responses.add(service.pauseOrder(ORDER_ID, "pause"));
		responses.add(service.reactivateOrder(ORDER_ID));
		responses.add(changeStatus(null, null, null, null));

		assertThat(responses).hasSize(6);
		for (OrderDTO dto : responses) {
			JsonNode node = json(dto);
			assertThat(node.has("printFilesUrl")).isTrue();
			assertThat(node.get("printFilesUrl").isNull()).isTrue();
		}
	}

	// --- Visibility ---

	@ParameterizedTest
	@ValueSource(strings = {"company_admin", "manager", "manufacturer"})
	void allAuthenticatedRolesSeeTheLink(String roleName) {
		orderAt(OrderStatus.PRINT_READY, "https://example.com/folder");
		role(roleName);

		OrderDTO dto = service.getOrderById(ORDER_ID);

		assertThat(dto.getPrintFilesUrl()).isEqualTo("https://example.com/folder");
	}

	@Test
	void linkIsVisibleToNonAdminEvenWhenTheOrderNoteIsInternal() {
		orderAt(OrderStatus.PRINT_READY, "https://example.com/folder");
		order.setInternalNote(true);
		role("manufacturer");

		OrderDTO dto = service.getOrderById(ORDER_ID);

		assertThat(dto.getNote()).isNull();
		assertThat(dto.getPrintFilesUrl()).isEqualTo("https://example.com/folder");
	}

	// --- Update and create isolation ---

	private static OrderRecord updateBody(String evilUrl) {
		OrderRecord body = incoming();
		body.setPrintFilesUrl(evilUrl);
		return body;
	}

	@Test
	void putOrderIgnoresPrintFilesUrlInTheBodyWhenALinkIsStored() {
		orderAt(OrderStatus.PRINT_READY, "https://example.com/original");

		OrderDTO dto = service.updateOrder(updateBody("https://evil.example/x"));

		assertThat(dto.getPrintFilesUrl()).isEqualTo("https://example.com/original");
		assertThat(order.getPrintFilesUrl()).isEqualTo("https://example.com/original");
	}

	@Test
	void putOrderDoesNotSetALinkWhenNoneIsStored() {
		orderAt(OrderStatus.PRINT_READY, null);

		OrderDTO dto = service.updateOrder(updateBody("https://evil.example/x"));

		assertThat(dto.getPrintFilesUrl()).isNull();
		assertThat(order.getPrintFilesUrl()).isNull();
	}

	@Test
	void putOrderDoesNotTouchThePrintReadyHistoryEntry() {
		orderAt(OrderStatus.PRINT_READY, "https://example.com/original");

		service.updateOrder(updateBody(null));

		assertThat(printReadyEntry().getPrintFilesUrl()).isEqualTo("https://example.com/original");
	}

	@Test
	void postCreateIgnoresPrintFilesUrlInTheBody() {
		OrderDTO dto = service.createOrder(updateBody("https://example.com/x"));

		assertThat(dto.getPrintFilesUrl()).isNull();
		org.mockito.ArgumentCaptor<OrderRecord> captor = org.mockito.ArgumentCaptor.forClass(OrderRecord.class);
		org.mockito.Mockito.verify(orderRepository).save(captor.capture());
		assertThat(captor.getValue().getPrintFilesUrl()).isNull();
	}

	// --- Edit endpoint (service part) ---

	@Test
	void adminSetsTheLinkOnAnOrderAtPrintReady() {
		orderAt(OrderStatus.PRINT_READY, null);

		OrderDTO dto = service.editPrintFilesUrl(ORDER_ID, editDto(NEW));

		assertThat(dto.getPrintFilesUrl()).isEqualTo(NEW);
		assertThat(printReadyEntry().getPrintFilesUrl()).isEqualTo(NEW);
		assertThat(order.getPrintFilesUrl()).isEqualTo(NEW);
	}

	@Test
	void adminReplacesAnExistingLinkAndThePrintReadyHistoryEntryIsOverwritten() {
		orderAt(OrderStatus.SEWING, OLD);
		int entries = history.size();

		OrderDTO dto = service.editPrintFilesUrl(ORDER_ID, editDto(NEW));

		assertThat(dto.getPrintFilesUrl()).isEqualTo(NEW);
		assertThat(printReadyEntry().getPrintFilesUrl()).isEqualTo(NEW);
		assertThat(history).hasSize(entries);
	}

	@Test
	void editIsAllowedOnADoneOrder() {
		orderAt(OrderStatus.DONE, OLD);

		OrderDTO dto = service.editPrintFilesUrl(ORDER_ID, editDto(NEW));

		assertThat(dto.getPrintFilesUrl()).isEqualTo(NEW);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"  "})
	void nullOrBlankClearsTheLink(String value) {
		orderAt(OrderStatus.PRINTING, OLD);

		OrderDTO dto = service.editPrintFilesUrl(ORDER_ID, editDto(value));

		JsonNode node = json(dto);
		assertThat(node.has("printFilesUrl")).isTrue();
		assertThat(node.get("printFilesUrl").isNull()).isTrue();
		assertThat(order.getPrintFilesUrl()).isNull();
		assertThat(printReadyEntry().getPrintFilesUrl()).isNull();
	}

	@ParameterizedTest
	@EnumSource(value = OrderStatus.class, names = {"PENDING", "DESIGN"})
	void editBeforePrintReadyReturns400(OrderStatus status) {
		orderAt(status, null);

		assertThatThrownBy(() -> service.editPrintFilesUrl(ORDER_ID, editDto("https://example.com/x")))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(order.getPrintFilesUrl()).isNull();
	}

	@Test
	void editRejectsAnInvalidLinkWith400AndChangesNothing() {
		orderAt(OrderStatus.PRINTING, OLD);

		assertThatThrownBy(() -> service.editPrintFilesUrl(ORDER_ID, editDto("javascript:alert(1)")))
				.isInstanceOf(IllegalArgumentException.class)
				.satisfies(e -> assertProblemDetail((IllegalArgumentException) e));

		assertThat(order.getPrintFilesUrl()).isEqualTo(OLD);
		assertThat(printReadyEntry().getPrintFilesUrl()).isEqualTo(OLD);
	}

	@Test
	void editDoesNotChangeStatusOrExecutionStatus() {
		orderAt(OrderStatus.PRINTING, OLD);
		order.setExecutionStatus(OrderExecutionStatus.PAUSED);

		service.editPrintFilesUrl(ORDER_ID, editDto("https://example.com/x"));

		assertThat(order.getStatus()).isEqualTo(OrderStatus.PRINTING);
		assertThat(order.getExecutionStatus()).isEqualTo(OrderExecutionStatus.PAUSED);
	}

	/**
	 * SPEC DISCREPANCY: the scenario says 404, but the existing findOrderForCurrentTenant helper
	 * (TenantGuard) throws AccessDeniedException for a cross-tenant order, which
	 * GlobalExceptionHandler maps to 403. The test pins the real behaviour: access is denied
	 * and nothing is changed.
	 */
	@Test
	void editOnAnotherTenantsOrderIsDeniedByTheExistingTenantGuard() {
		orderAt(OrderStatus.PRINTING, OLD);
		Tenant other = new Tenant("Other", "other");
		other.setId(2L);
		order.setTenant(other);

		assertThatThrownBy(() -> service.editPrintFilesUrl(ORDER_ID, editDto(NEW)))
				.isInstanceOf(AccessDeniedException.class);

		assertThat(order.getPrintFilesUrl()).isEqualTo(OLD);
	}

	@Test
	void editOnASoftDeletedOrderReturns404() {
		// @SQLRestriction("deleted = false") makes findById return empty for soft-deleted rows
		when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.editPrintFilesUrl(ORDER_ID, editDto(NEW)))
				.isInstanceOf(OrderNotFoundException.class);
	}
}

package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.repository.OrderRepository;
import cbd.order_tracker.repository.OrderStatusHistoryRepository;
import cbd.order_tracker.repository.PaymentRepository;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.util.UserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplInternalNoteTest {

	private static final Long TENANT_ID = 1L;
	private static final Long ORDER_ID = 10L;

	private OrderRepository orderRepository;
	private TenantRepository tenantRepository;
	private UserUtil userUtil;
	private OrderStatusHistoryRepository historyRepository;
	private OrderServiceImpl service;
	private Tenant tenant;

	@BeforeEach
	void setup() {
		orderRepository = mock(OrderRepository.class);
		tenantRepository = mock(TenantRepository.class);
		userUtil = mock(UserUtil.class);
		historyRepository = mock(OrderStatusHistoryRepository.class);
		service = new OrderServiceImpl(userUtil, orderRepository, mock(PaymentRepository.class),
				historyRepository, tenantRepository);
		tenant = new Tenant("CBD", "cbd");
		tenant.setId(TENANT_ID);
		TenantContext.setSuperadmin(false);
		TenantContext.setTenantId(TENANT_ID);
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(orderRepository.save(any(OrderRecord.class))).thenAnswer(i -> i.getArgument(0));
		when(historyRepository.findByOrderId(ORDER_ID)).thenReturn(List.of());
	}

	@AfterEach
	void cleanup() {
		TenantContext.clear();
	}

	private void currentRole(String roleName) {
		when(userUtil.getCurrentUserRoles()).thenReturn(Set.of(new Role(roleName)));
	}

	private static OrderRecord incoming(String name, String note, Boolean internalNote) {
		OrderRecord o = new OrderRecord();
		o.setId(ORDER_ID);
		o.setName(name);
		o.setNote(note);
		o.setInternalNote(internalNote);
		o.setSalePrice(new BigDecimal("100"));
		o.setAcquisitionCost(new BigDecimal("40"));
		return o;
	}

	private OrderRecord stored(String name, String note, boolean internalNote) {
		OrderRecord r = new OrderRecord(incoming(name, note, internalNote));
		r.setId(ORDER_ID);
		r.setTenant(tenant);
		when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(r));
		return r;
	}

	private OrderRecord savedRecord() {
		ArgumentCaptor<OrderRecord> captor = ArgumentCaptor.forClass(OrderRecord.class);
		verify(orderRepository).save(captor.capture());
		return captor.getValue();
	}

	@Test
	void adminCreatesOrderWithInternalNote() {
		currentRole("company_admin");

		OrderDTO dto = service.createOrder(incoming("A", "Secret", true));

		assertThat(savedRecord().getInternalNote()).isTrue();
		assertThat(dto.getNote()).isEqualTo("Secret");
		assertThat(dto.getInternalNote()).isTrue();
	}

	@Test
	void adminCreatesOrderOmittingInternalNote() {
		currentRole("company_admin");

		service.createOrder(incoming("A", "Secret", null));

		assertThat(savedRecord().getInternalNote()).isFalse();
	}

	@Test
	void nonAdminCannotCreateInternalNote() {
		currentRole("manufacturer");

		OrderDTO dto = service.createOrder(incoming("A", "Secret", true));

		assertThat(savedRecord().getInternalNote()).isFalse();
		assertThat(dto.getNote()).isEqualTo("Secret");
		assertThat(dto.getInternalNote()).isNull();
	}

	@Test
	void nonAdminCreatesOrderWithNormalNote() {
		currentRole("manager");

		service.createOrder(incoming("A", "Hello", false));

		OrderRecord saved = savedRecord();
		assertThat(saved.getNote()).isEqualTo("Hello");
		assertThat(saved.getInternalNote()).isFalse();
	}

	@Test
	void adminFlipsFlagFromFalseToTrueOnUpdate() {
		currentRole("company_admin");
		stored("A", "Old", false);

		service.updateOrder(incoming("A", "New", true));

		OrderRecord saved = savedRecord();
		assertThat(saved.getNote()).isEqualTo("New");
		assertThat(saved.getInternalNote()).isTrue();
	}

	@Test
	void adminFlipsFlagFromTrueToFalseOnUpdate() {
		currentRole("company_admin");
		stored("A", "Old", true);

		service.updateOrder(incoming("A", "New", false));

		OrderRecord saved = savedRecord();
		assertThat(saved.getNote()).isEqualTo("New");
		assertThat(saved.getInternalNote()).isFalse();
	}

	@Test
	void adminOmitsInternalNoteOnUpdateWithStoredTrue() {
		currentRole("company_admin");
		stored("A", "Old", true);

		service.updateOrder(incoming("A", "New", null));

		OrderRecord saved = savedRecord();
		assertThat(saved.getNote()).isEqualTo("New");
		assertThat(saved.getInternalNote()).isTrue();
	}

	@Test
	void adminOmitsInternalNoteOnUpdateWithStoredFalse() {
		currentRole("company_admin");
		stored("A", "Old", false);

		service.updateOrder(incoming("A", "New", null));

		OrderRecord saved = savedRecord();
		assertThat(saved.getNote()).isEqualTo("New");
		assertThat(saved.getInternalNote()).isFalse();
	}

	@Test
	void nonAdminCannotEditInternalNoteOrFlag() {
		currentRole("manufacturer");
		stored("A", "Secret", true);

		OrderDTO dto = service.updateOrder(incoming("B", "Hacked", false));

		OrderRecord saved = savedRecord();
		assertThat(saved.getNote()).isEqualTo("Secret");
		assertThat(saved.getInternalNote()).isTrue();
		assertThat(saved.getName()).isEqualTo("B");
		assertThat(dto.getNote()).isNull();
		assertThat(dto.getInternalNote()).isNull();
	}

	@Test
	void nonAdminEditsNonInternalNoteButCannotSetFlag() {
		currentRole("manager");
		stored("A", "Old", false);

		OrderDTO dto = service.updateOrder(incoming("A", "New", true));

		OrderRecord saved = savedRecord();
		assertThat(saved.getNote()).isEqualTo("New");
		assertThat(saved.getInternalNote()).isFalse();
		assertThat(dto.getNote()).isEqualTo("New");
		assertThat(dto.getInternalNote()).isNull();
	}
}

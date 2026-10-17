package cbd.order_tracker.util;

import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderHistoryEventType;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.OrderStatusHistory;
import cbd.order_tracker.model.dto.OrderStatusHistoryDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperPreviousExecutionStatusTest {

	private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 1, 10, 0);

	private static OrderStatusHistory workflow(long id, OrderStatus status, LocalDateTime time) {
		OrderStatusHistory h = new OrderStatusHistory();
		h.setId(id);
		h.setStatus(status);
		h.setCreationTime(time);
		return h;
	}

	private static OrderStatusHistory execution(long id, OrderExecutionStatus status, LocalDateTime time) {
		OrderStatusHistory h = new OrderStatusHistory();
		h.setId(id);
		h.setExecutionStatus(status);
		h.setCreationTime(time);
		return h;
	}

	// --- Scenario: Workflow-status rows are ignored when deriving previous execution status
	@Test
	void workflowStatusRowsAreIgnoredWhenDerivingPreviousExecutionStatus() {
		List<OrderStatusHistoryDTO> dtos = OrderMapper.mapStatusHistory(List.of(
				workflow(1, OrderStatus.DESIGN, T0),
				execution(2, OrderExecutionStatus.PAUSED, T0.plusMinutes(1)),
				workflow(3, OrderStatus.PRINT_READY, T0.plusMinutes(2)),
				execution(4, OrderExecutionStatus.ACTIVE, T0.plusMinutes(3))));

		assertThat(dtos.get(1).getPreviousExecutionStatus()).isEqualTo(OrderExecutionStatus.ACTIVE);
		assertThat(dtos.get(3).getPreviousExecutionStatus()).isEqualTo(OrderExecutionStatus.PAUSED);
		assertThat(dtos.get(0).getPreviousExecutionStatus()).isNull();
		assertThat(dtos.get(2).getPreviousExecutionStatus()).isNull();
	}

	// --- Scenario: Rows with identical creationTime are ordered by id for derivation
	@Test
	void rowsWithIdenticalCreationTimeAreOrderedByIdForDerivation() {
		// supplied in reverse id order on purpose
		List<OrderStatusHistoryDTO> dtos = OrderMapper.mapStatusHistory(List.of(
				execution(2, OrderExecutionStatus.ACTIVE, T0),
				execution(1, OrderExecutionStatus.PAUSED, T0)));

		assertThat(dtos).filteredOn(d -> d.getId() == 1).singleElement()
				.satisfies(d -> assertThat(d.getPreviousExecutionStatus()).isEqualTo(OrderExecutionStatus.ACTIVE));
		assertThat(dtos).filteredOn(d -> d.getId() == 2).singleElement()
				.satisfies(d -> assertThat(d.getPreviousExecutionStatus()).isEqualTo(OrderExecutionStatus.PAUSED));
	}

	// --- Scenario: Derivation does not reorder statusHistory and non-execution rows get null
	@Test
	void derivationDoesNotReorderStatusHistoryAndNonExecutionRowsGetNull() {
		OrderStatusHistory combined = new OrderStatusHistory();
		combined.setId(3L);
		combined.setEventType(OrderHistoryEventType.COMBINED);
		combined.setCreationTime(T0.plusMinutes(5));
		// stored order deliberately not chronological
		List<OrderStatusHistory> stored = List.of(
				execution(5, OrderExecutionStatus.CANCELED, T0.plusMinutes(9)),
				workflow(1, OrderStatus.DESIGN, T0),
				combined,
				execution(4, OrderExecutionStatus.PAUSED, T0.plusMinutes(1)));

		List<OrderStatusHistoryDTO> dtos = OrderMapper.mapStatusHistory(stored);

		assertThat(dtos).extracting(OrderStatusHistoryDTO::getId).containsExactly(5L, 1L, 3L, 4L);
		assertThat(dtos.get(2).getPreviousExecutionStatus()).isNull();
		assertThat(dtos.get(0).getPreviousExecutionStatus()).isEqualTo(OrderExecutionStatus.PAUSED);
		assertThat(dtos.get(3).getPreviousExecutionStatus()).isEqualTo(OrderExecutionStatus.ACTIVE);
	}
}

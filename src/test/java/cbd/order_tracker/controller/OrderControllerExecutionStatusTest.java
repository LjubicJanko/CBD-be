package cbd.order_tracker.controller;

import cbd.order_tracker.config.FeatureGuard;
import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.ChangeExecutionStatusRequestDto;
import cbd.order_tracker.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The admin-only rule lives in the service (R1), so the controller must stay a plain pass-through for all
 * three execution-status endpoints and must not swallow the service's AccessDeniedException (the existing
 * GlobalExceptionHandler turns it into a 403).
 */
class OrderControllerExecutionStatusTest {

	private final OrderService service = mock(OrderService.class);
	private final OrderController controller = new OrderController(service, mock(FeatureGuard.class));

	@Test
	void changeExecutionStatusPassesBodyToTheServiceAndPropagatesDenial() {
		ChangeExecutionStatusRequestDto request = new ChangeExecutionStatusRequestDto();
		request.setExecutionStatus(OrderExecutionStatus.ACTIVE);
		request.setNote("");
		OrderDTO dto = new OrderDTO();
		when(service.changeExecutionStatus(100L, OrderExecutionStatus.ACTIVE, "")).thenReturn(dto);

		assertThat(controller.changeExecutionStatus(100L, request)).isSameAs(dto);

		when(service.changeExecutionStatus(100L, OrderExecutionStatus.ACTIVE, ""))
				.thenThrow(new AccessDeniedException("denied"));
		assertThatThrownBy(() -> controller.changeExecutionStatus(100L, request))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void pauseAndReactivateDelegateToTheService() {
		OrderDTO paused = new OrderDTO();
		OrderDTO reactivated = new OrderDTO();
		when(service.pauseOrder(100L, "comment")).thenReturn(paused);
		when(service.reactivateOrder(100L)).thenReturn(reactivated);

		assertThat(controller.pauseOrder(100L, "comment")).isSameAs(paused);
		assertThat(controller.reactivateOrder(100L)).isSameAs(reactivated);
		verify(service).pauseOrder(100L, "comment");
		verify(service).reactivateOrder(100L);
	}
}

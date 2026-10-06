package cbd.order_tracker.controller;

import cbd.order_tracker.config.FeatureGuard;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.request.EditPrintFilesUrlDto;
import cbd.order_tracker.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Both tests verify the {@code @PreAuthorize} annotation by reflection. They do NOT verify the
 * Spring Security interception itself (not evaluated in plain unit tests or MockMvc standaloneSetup,
 * and no database-free security slice is available here).
 */
class OrderControllerPrintFilesUrlTest {

	private static final String EXPECTED = "hasAnyRole('company_admin','SUPERADMIN')";

	private static PreAuthorize annotation() throws NoSuchMethodException {
		Method m = OrderController.class.getMethod("editPrintFilesUrl", Long.class, EditPrintFilesUrlDto.class);
		return m.getAnnotation(PreAuthorize.class);
	}

	@Test
	void nonAdminCannotEditTheLinkAnnotationRestrictsToAdminRoles() throws NoSuchMethodException {
		PreAuthorize pre = annotation();

		assertThat(pre).isNotNull();
		assertThat(pre.value()).isEqualTo(EXPECTED);
	}

	@Test
	void superadminCanEditTheLinkExpressionIncludesSuperadminRole() throws NoSuchMethodException {
		assertThat(annotation().value()).contains("'SUPERADMIN'");
	}

	// --- controller pass-through (mocked service) ---

	private final OrderService service = mock(OrderService.class);
	private final OrderController controller = new OrderController(service, mock(FeatureGuard.class));

	@Test
	void changeStatusPassesAllBodyValuesIncludingPrintFilesUrlToTheService() {
		OrderDTO dto = new OrderDTO();
		when(service.changeStatus(5L, "ready", "ABC123", "post", "https://example.com/f")).thenReturn(dto);
		Map<String, String> body = new HashMap<>();
		body.put("closingComment", "ready");
		body.put("postalCode", "ABC123");
		body.put("postalService", "post");
		body.put("printFilesUrl", "https://example.com/f");

		OrderDTO result = controller.changeStatus(5L, body);

		assertThat(result).isSameAs(dto);
		verify(service).changeStatus(5L, "ready", "ABC123", "post", "https://example.com/f");
	}

	@Test
	void changeStatusPassesNullPrintFilesUrlWhenTheKeyIsAbsent() {
		controller.changeStatus(5L, new HashMap<>());

		verify(service).changeStatus(5L, null, null, null, null);
	}

	@Test
	void changeStatusPassesABlankPrintFilesUrlThroughUnchanged() {
		controller.changeStatus(5L, Map.of("printFilesUrl", "   "));

		verify(service).changeStatus(5L, null, null, null, "   ");
	}

	@Test
	void editPrintFilesUrlReturns200WithTheServiceDtoAndPassesIdAndUrlThrough() {
		OrderDTO dto = new OrderDTO();
		EditPrintFilesUrlDto request = new EditPrintFilesUrlDto();
		request.setPrintFilesUrl("https://example.com/new");
		when(service.editPrintFilesUrl(7L, request)).thenReturn(dto);

		ResponseEntity<OrderDTO> response = controller.editPrintFilesUrl(7L, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isSameAs(dto);
		verify(service).editPrintFilesUrl(7L, request);
		assertThat(request.getPrintFilesUrl()).isEqualTo("https://example.com/new");
	}
}

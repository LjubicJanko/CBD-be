package cbd.order_tracker.util;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.OrderStatusHistory;
import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.OrderOverviewDto;
import cbd.order_tracker.model.dto.OrderTrackingDTO;
import cbd.order_tracker.model.dto.response.OrderExtensionDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperPrintFilesUrlTest {

	private static final String LINK = "https://example.com/folder";

	private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

	@BeforeEach
	void setup() {
		TenantContext.setSuperadmin(false);
	}

	@AfterEach
	void cleanup() {
		TenantContext.clear();
	}

	private static OrderRecord record() {
		OrderRecord req = new OrderRecord();
		req.setName("Jersey");
		req.setSalePrice(new BigDecimal("100"));
		req.setAcquisitionCost(new BigDecimal("40"));
		return new OrderRecord(req);
	}

	private static boolean hasProperty(Class<?> type, String name) {
		return java.util.Arrays.stream(type.getDeclaredFields()).anyMatch(f -> f.getName().equals(name))
				|| java.util.Arrays.stream(type.getMethods()).anyMatch(m -> m.getName().equalsIgnoreCase("get" + name));
	}

	@Test
	void everyStatusHistoryEntryContainsThePrintFilesUrlKeyAndOnlyPrintReadyHasAValue() {
		OrderRecord r = record();
		List<OrderStatusHistory> history = new ArrayList<>();
		history.add(new OrderStatusHistory(r, OrderStatus.DESIGN, null, null));
		OrderStatusHistory printReady = new OrderStatusHistory(r, OrderStatus.PRINT_READY, null, null);
		printReady.setPrintFilesUrl(LINK);
		history.add(printReady);
		history.add(new OrderStatusHistory(r, OrderStatus.PRINTING, null, null));
		r.setPrintFilesUrl(LINK);

		OrderDTO dto = OrderMapper.toDto(r, history, List.of(new Role("manager")));
		JsonNode entries = mapper.valueToTree(dto).get("statusHistory");

		assertThat(entries).hasSize(3);
		for (JsonNode entry : entries) {
			assertThat(entry.has("printFilesUrl")).isTrue();
		}
		assertThat(entries.get(0).get("printFilesUrl").isNull()).isTrue();
		assertThat(entries.get(1).get("printFilesUrl").asText()).isEqualTo(LINK);
		assertThat(entries.get(2).get("printFilesUrl").isNull()).isTrue();
	}

	@Test
	void overviewListRowsContainThePrintFilesUrlKey() {
		OrderRecord withLink = record();
		withLink.setPrintFilesUrl(LINK);
		OrderRecord without = record();

		// entity constructor (searchOrders) and JPQL projection constructor (fetchPageable)
		List<OrderOverviewDto> rows = List.of(
				new OrderOverviewDto(withLink),
				new OrderOverviewDto(without),
				new OrderOverviewDto(1L, "A", "d", null, OrderStatus.PRINT_READY, null, null, null, null, null,
						new BigDecimal("100"), new BigDecimal("120"), false, BigDecimal.ZERO, false, LINK),
				new OrderOverviewDto(2L, "B", "d", null, OrderStatus.DESIGN, null, null, null, null, null,
						new BigDecimal("100"), new BigDecimal("120"), false, BigDecimal.ZERO, false, null));

		for (OrderOverviewDto row : rows) {
			assertThat(mapper.valueToTree(row).has("printFilesUrl")).isTrue();
		}
		assertThat(rows.get(0).getPrintFilesUrl()).isEqualTo(LINK);
		assertThat(rows.get(1).getPrintFilesUrl()).isNull();
		assertThat(rows.get(2).getPrintFilesUrl()).isEqualTo(LINK);
		assertThat(rows.get(3).getPrintFilesUrl()).isNull();
		assertThat(mapper.valueToTree(rows.get(1)).get("printFilesUrl").isNull()).isTrue();
	}

	@Test
	void publicTrackEndpointNeverExposesTheLink() {
		OrderRecord r = record();
		r.setStatus(OrderStatus.PRINT_READY);
		r.setPrintFilesUrl(LINK);
		r.getStatusHistory().get(0).setPrintFilesUrl(LINK);

		OrderTrackingDTO dto = OrderMapper.toOrderTrackingDTO(r);

		assertThat(mapper.valueToTree(dto).has("printFilesUrl")).isFalse();
		assertThat(mapper.valueToTree(dto).toString()).doesNotContain(LINK);
		assertThat(hasProperty(OrderTrackingDTO.class, "printFilesUrl")).isFalse();
	}

	@Test
	void publicOrderExtendDtosNeverExposeTheLink() {
		OrderRecord r = record();
		r.setPrintFilesUrl(LINK);

		OrderExtensionDto dto = OrderExtensionMapper.toDto(r);

		assertThat(mapper.valueToTree(dto).has("printFilesUrl")).isFalse();
		assertThat(mapper.valueToTree(dto).toString()).doesNotContain(LINK);
		assertThat(hasProperty(OrderExtensionDto.class, "printFilesUrl")).isFalse();
	}

	@Test
	void orderRecordCopyConstructorDoesNotCopyPrintFilesUrl() {
		OrderRecord source = record();
		source.setPrintFilesUrl("https://example.com/x");

		OrderRecord copy = new OrderRecord(source);

		assertThat(copy.getPrintFilesUrl()).isNull();
	}
}

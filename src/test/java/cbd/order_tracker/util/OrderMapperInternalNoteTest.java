package cbd.order_tracker.util;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.OrderOverviewDto;
import cbd.order_tracker.model.dto.OrderTrackingDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperInternalNoteTest {

	private static final String NOTE = "Call before shipping";

	@BeforeEach
	void setup() {
		TenantContext.setSuperadmin(false);
	}

	@AfterEach
	void cleanup() {
		TenantContext.clear();
	}

	private static OrderRecord record(String note, Boolean internalNote) {
		OrderRecord req = new OrderRecord();
		req.setName("Jersey");
		req.setSalePrice(new BigDecimal("100"));
		req.setAcquisitionCost(new BigDecimal("40"));
		OrderRecord r = new OrderRecord(req);
		r.setNote(note);
		r.setInternalNote(internalNote);
		return r;
	}

	private static OrderDTO map(OrderRecord r, String roleName) {
		return OrderMapper.toDto(r, List.of(), List.of(new Role(roleName)));
	}

	@Test
	void adminSeesNoteAndFlagViaMapper() {
		OrderDTO dto = map(record(NOTE, true), "company_admin");

		assertThat(dto.getNote()).isEqualTo(NOTE);
		assertThat(dto.getInternalNote()).isTrue();
	}

	@Test
	void superadminSeesNoteAndFlagViaMapper() {
		TenantContext.setSuperadmin(true);

		OrderDTO dto = map(record(NOTE, true), "manufacturer");

		assertThat(dto.getNote()).isEqualTo(NOTE);
		assertThat(dto.getInternalNote()).isTrue();
	}

	@Test
	void adminSeesNullNoteAsNullWithFlagFalse() {
		OrderDTO dto = map(record(null, false), "company_admin");

		assertThat(dto.getNote()).isNull();
		assertThat(dto.getInternalNote()).isFalse();
	}

	@Test
	void manufacturerSeesNonInternalNoteWithoutFlag() {
		OrderDTO dto = map(record(NOTE, false), "manufacturer");

		assertThat(dto.getNote()).isEqualTo(NOTE);
		assertThat(dto.getInternalNote()).isNull();
	}

	@Test
	void manufacturerSeesEmptyStringForNullNonInternalNote() {
		OrderDTO dto = map(record(null, false), "manufacturer");

		assertThat(dto.getNote()).isEmpty();
		assertThat(dto.getInternalNote()).isNull();
	}

	@Test
	void manufacturerDoesNotSeeInternalNote() {
		OrderDTO dto = map(record(NOTE, true), "manufacturer");

		assertThat(dto.getNote()).isNull();
		assertThat(dto.getInternalNote()).isNull();
	}

	@Test
	void descriptionReturnedToNonAdminsEvenWhenNoteIsInternal() {
		OrderRecord r = record(NOTE, true);
		r.setDescription("Blue jersey");

		OrderDTO dto = map(r, "manufacturer");

		assertThat(dto.getDescription()).isEqualTo("Blue jersey");
	}

	@Test
	void managerIsTreatedExactlyLikeManufacturer() {
		OrderDTO internal = map(record(NOTE, true), "manager");
		assertThat(internal.getNote()).isNull();
		assertThat(internal.getInternalNote()).isNull();

		OrderDTO managerOpen = map(record(NOTE, false), "manager");
		OrderDTO manufacturerOpen = map(record(NOTE, false), "manufacturer");
		assertThat(managerOpen.getNote()).isEqualTo(manufacturerOpen.getNote());
		assertThat(managerOpen.getInternalNote()).isEqualTo(manufacturerOpen.getInternalNote());
	}

	@Test
	void trackingAndOverviewDtosNeverCarryNote() {
		OrderRecord r = record(NOTE, true);

		for (Class<?> type : List.of(OrderTrackingDTO.class, OrderOverviewDto.class)) {
			assertThat(Arrays.stream(type.getDeclaredFields()).map(Field::getName))
					.doesNotContain("note", "internalNote");
			assertThat(Arrays.stream(type.getMethods()).map(Method::getName))
					.doesNotContain("getNote", "getInternalNote", "isInternalNote");
		}
		// constructing them from a record with an internal note must still work
		assertThat(new OrderTrackingDTO(r)).isNotNull();
		assertThat(new OrderOverviewDto(r)).isNotNull();
	}

	@Test
	void nullStoredFlagIsTreatedAsFalseByMapper() {
		OrderRecord r = record(NOTE, null);

		OrderDTO admin = map(r, "company_admin");
		assertThat(admin.getInternalNote()).isFalse();

		OrderDTO manufacturer = map(r, "manufacturer");
		assertThat(manufacturer.getNote()).isEqualTo(NOTE);
		assertThat(manufacturer.getInternalNote()).isNull();
	}
}

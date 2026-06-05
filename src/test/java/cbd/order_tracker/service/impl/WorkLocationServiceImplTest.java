package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.WorkLocation;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.repository.WorkLocationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkLocationServiceImplTest {

	private static final Long TENANT_ID = 1L;

	private WorkLocationRepository locationRepo;
	private TenantRepository tenantRepo;
	private WorkLocationServiceImpl service;
	private Tenant tenant;

	@BeforeEach
	void setup() {
		locationRepo = mock(WorkLocationRepository.class);
		tenantRepo = mock(TenantRepository.class);
		service = new WorkLocationServiceImpl(locationRepo, tenantRepo);
		tenant = new Tenant("CBD", "cbd");
		tenant.setId(TENANT_ID);
		lenient().when(tenantRepo.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		lenient().when(locationRepo.save(any(WorkLocation.class))).thenAnswer(i -> i.getArgument(0));
		TenantContext.setTenantId(TENANT_ID);
	}

	@AfterEach
	void tearDown() {
		TenantContext.clear();
	}

	@Test
	void delete_softDeletesWhenSessionsExist() {
		WorkLocation loc = new WorkLocation();
		loc.setId(5L);
		loc.setTenant(tenant);
		loc.setActive(true);
		loc.setName("HQ");
		loc.setLat(BigDecimal.ONE);
		loc.setLng(BigDecimal.ONE);
		loc.setRadiusM(100);

		when(locationRepo.findByIdAndTenant(5L, TENANT_ID)).thenReturn(Optional.of(loc));
		when(locationRepo.countSessionsForLocation(5L)).thenReturn(3L);

		service.delete(5L);

		verify(locationRepo).save(loc);
		verify(locationRepo, never()).delete(any());
		assertThat(loc.isActive()).isFalse();
	}

	@Test
	void delete_hardDeletesWhenNoSessions() {
		WorkLocation loc = new WorkLocation();
		loc.setId(5L);
		loc.setTenant(tenant);
		loc.setActive(true);
		loc.setName("HQ");
		loc.setLat(BigDecimal.ONE);
		loc.setLng(BigDecimal.ONE);
		loc.setRadiusM(100);

		when(locationRepo.findByIdAndTenant(5L, TENANT_ID)).thenReturn(Optional.of(loc));
		when(locationRepo.countSessionsForLocation(5L)).thenReturn(0L);

		service.delete(5L);

		verify(locationRepo).delete(eq(loc));
		verify(locationRepo, never()).save(any());
	}
}

package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.FeatureGuard;
import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.CheckInMethod;
import cbd.order_tracker.model.enums.Feature;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.WorkLocation;
import cbd.order_tracker.model.dto.request.WorkLocationPatchRequest;
import cbd.order_tracker.model.dto.request.WorkLocationRequest;
import cbd.order_tracker.model.dto.response.WorkLocationDto;
import cbd.order_tracker.repository.AttendanceSessionRepository;
import cbd.order_tracker.repository.AttendanceSessionRepository.OpenSessionCount;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.repository.WorkLocationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
	private AttendanceSessionRepository sessionRepo;
	private WorkLocationServiceImpl service;
	private Tenant tenant;

	@BeforeEach
	void setup() {
		locationRepo = mock(WorkLocationRepository.class);
		tenantRepo = mock(TenantRepository.class);
		sessionRepo = mock(AttendanceSessionRepository.class);
		service = new WorkLocationServiceImpl(locationRepo, tenantRepo, sessionRepo, new FeatureGuard(tenantRepo));
		tenant = new Tenant("CBD", "cbd");
		tenant.setId(TENANT_ID);
		lenient().when(tenantRepo.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		lenient().when(locationRepo.save(any(WorkLocation.class))).thenAnswer(i -> i.getArgument(0));
		TenantContext.setTenantId(TENANT_ID);
		// Baseline: tenant has the geofence sub-feature enabled. Tests for the gate itself
		// override this explicitly.
		TenantContext.setFeatures(Set.of(Feature.ATTENDANCE_GEOFENCE.getKey()));
	}

	@AfterEach
	void tearDown() {
		TenantContext.clear();
		SecurityContextHolder.clearContext();
	}

	private void authenticateAs(String... authorities) {
		var grants = java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("tester", null, grants));
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

	// === QR check-in method ===

	private WorkLocationRequest geofenceRequest(BigDecimal lat, BigDecimal lng, Integer radiusM) {
		WorkLocationRequest req = new WorkLocationRequest();
		req.setName("HQ");
		req.setCheckInMethod(CheckInMethod.GEOFENCE);
		req.setLat(lat);
		req.setLng(lng);
		req.setRadiusM(radiusM);
		return req;
	}

	private WorkLocationRequest qrRequest() {
		WorkLocationRequest req = new WorkLocationRequest();
		req.setName("Warehouse");
		req.setCheckInMethod(CheckInMethod.QR);
		return req;
	}

	@Test
	void create_geofence_missingFields_throws() {
		assertThatThrownBy(() -> service.create(geofenceRequest(BigDecimal.ONE, null, 100)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void create_geofence_success_leavesQrTokenNull() {
		WorkLocationDto dto = service.create(geofenceRequest(BigDecimal.ONE, BigDecimal.TEN, 100));

		assertThat(dto.getCheckInMethod()).isEqualTo(CheckInMethod.GEOFENCE);
		assertThat(dto.getQrToken()).isNull();
	}

	// === attendance-geofence feature gate ===

	@Test
	void create_geofence_withoutFeature_throws() {
		TenantContext.setFeatures(Set.of());

		assertThatThrownBy(() -> service.create(geofenceRequest(BigDecimal.ONE, BigDecimal.TEN, 100)))
				.isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
	}

	@Test
	void create_geofence_featureNotYetLoaded_nullFeatures_throws() {
		TenantContext.setFeatures(null);

		assertThatThrownBy(() -> service.create(geofenceRequest(BigDecimal.ONE, BigDecimal.TEN, 100)))
				.isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
	}

	@Test
	void create_geofence_superadminBypassesFeatureGate() {
		TenantContext.setFeatures(Set.of());
		TenantContext.setSuperadmin(true);

		WorkLocationDto dto = service.create(geofenceRequest(BigDecimal.ONE, BigDecimal.TEN, 100));

		assertThat(dto.getCheckInMethod()).isEqualTo(CheckInMethod.GEOFENCE);
		assertThat(dto.getLat()).isNotNull();
	}

	@Test
	void update_switchToGeofence_withoutFeature_throws() {
		WorkLocation loc = new WorkLocation();
		loc.setId(5L);
		loc.setTenant(tenant);
		loc.setName("Warehouse");
		loc.setCheckInMethod(CheckInMethod.QR);
		loc.setQrToken("existing-token");
		when(locationRepo.findByIdAndTenant(5L, TENANT_ID)).thenReturn(Optional.of(loc));
		TenantContext.setFeatures(Set.of());

		WorkLocationPatchRequest patch = new WorkLocationPatchRequest();
		patch.setCheckInMethod(CheckInMethod.GEOFENCE);
		patch.setLat(BigDecimal.ONE);
		patch.setLng(BigDecimal.TEN);
		patch.setRadiusM(100);

		assertThatThrownBy(() -> service.update(5L, patch))
				.isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
	}

	@Test
	void update_existingGeofenceLocation_remainsEditableWithoutFeature() {
		// The gate blocks *requesting* GEOFENCE, not merely *having* it — an already-GEOFENCE
		// location (e.g. one that predates this feature) must stay editable if the feature is
		// later disabled, since disabling must not lock admins out of unrelated edits (e.g. rename).
		WorkLocation loc = new WorkLocation();
		loc.setId(5L);
		loc.setTenant(tenant);
		loc.setName("HQ");
		loc.setCheckInMethod(CheckInMethod.GEOFENCE);
		loc.setLat(BigDecimal.ONE);
		loc.setLng(BigDecimal.TEN);
		loc.setRadiusM(100);
		when(locationRepo.findByIdAndTenant(5L, TENANT_ID)).thenReturn(Optional.of(loc));
		when(sessionRepo.countOpenByLocationForTenant(TENANT_ID)).thenReturn(List.of());
		TenantContext.setFeatures(Set.of());

		WorkLocationPatchRequest patch = new WorkLocationPatchRequest();
		patch.setName("HQ (renamed)");

		WorkLocationDto dto = service.update(5L, patch);

		assertThat(dto.getName()).isEqualTo("HQ (renamed)");
		assertThat(dto.getCheckInMethod()).isEqualTo(CheckInMethod.GEOFENCE);
	}

	@Test
	void create_qr_generatesUniqueToken_ignoresGeofenceFields() {
		when(locationRepo.existsByQrToken(anyString())).thenReturn(false);

		WorkLocationDto dto = service.create(qrRequest());

		assertThat(dto.getCheckInMethod()).isEqualTo(CheckInMethod.QR);
		assertThat(dto.getQrToken()).isNotBlank();
	}

	@Test
	void create_qr_retriesOnTokenCollision() {
		when(locationRepo.existsByQrToken(anyString())).thenReturn(true, false);

		WorkLocationDto dto = service.create(qrRequest());

		assertThat(dto.getQrToken()).isNotBlank();
		org.mockito.Mockito.verify(locationRepo, org.mockito.Mockito.times(2)).existsByQrToken(anyString());
	}

	@Test
	void update_switchToQr_generatesTokenOnce() {
		WorkLocation loc = new WorkLocation();
		loc.setId(5L);
		loc.setTenant(tenant);
		loc.setName("HQ");
		loc.setCheckInMethod(CheckInMethod.GEOFENCE);
		loc.setLat(BigDecimal.ONE);
		loc.setLng(BigDecimal.ONE);
		loc.setRadiusM(100);
		when(locationRepo.findByIdAndTenant(5L, TENANT_ID)).thenReturn(Optional.of(loc));
		when(locationRepo.existsByQrToken(anyString())).thenReturn(false);
		when(sessionRepo.countOpenByLocationForTenant(TENANT_ID)).thenReturn(List.of());

		WorkLocationPatchRequest patch = new WorkLocationPatchRequest();
		patch.setCheckInMethod(CheckInMethod.QR);
		WorkLocationDto dto = service.update(5L, patch);

		assertThat(dto.getQrToken()).isNotBlank();
		String firstToken = loc.getQrToken();

		// Re-patching (e.g. toggling active) must not mint a second token.
		WorkLocationPatchRequest noop = new WorkLocationPatchRequest();
		noop.setActive(true);
		service.update(5L, noop);
		assertThat(loc.getQrToken()).isEqualTo(firstToken);
	}

	@Test
	void update_switchToQr_clearsStaleGeofenceData() {
		WorkLocation loc = new WorkLocation();
		loc.setId(5L);
		loc.setTenant(tenant);
		loc.setName("HQ");
		loc.setCheckInMethod(CheckInMethod.GEOFENCE);
		loc.setLat(BigDecimal.ONE);
		loc.setLng(BigDecimal.ONE);
		loc.setRadiusM(100);
		when(locationRepo.findByIdAndTenant(5L, TENANT_ID)).thenReturn(Optional.of(loc));
		when(locationRepo.existsByQrToken(anyString())).thenReturn(false);
		when(sessionRepo.countOpenByLocationForTenant(TENANT_ID)).thenReturn(List.of());

		WorkLocationPatchRequest patch = new WorkLocationPatchRequest();
		patch.setCheckInMethod(CheckInMethod.QR);
		service.update(5L, patch);

		assertThat(loc.getLat()).isNull();
		assertThat(loc.getLng()).isNull();
		assertThat(loc.getRadiusM()).isNull();

		// Switching back to GEOFENCE without resupplying coordinates must not silently reuse
		// the stale data — it must fail the missing-fields check instead.
		WorkLocationPatchRequest backToGeofence = new WorkLocationPatchRequest();
		backToGeofence.setCheckInMethod(CheckInMethod.GEOFENCE);

		assertThatThrownBy(() -> service.update(5L, backToGeofence))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void update_switchToGeofence_missingFields_throws() {
		WorkLocation loc = new WorkLocation();
		loc.setId(5L);
		loc.setTenant(tenant);
		loc.setName("Warehouse");
		loc.setCheckInMethod(CheckInMethod.QR);
		loc.setQrToken("existing-token");
		when(locationRepo.findByIdAndTenant(5L, TENANT_ID)).thenReturn(Optional.of(loc));

		WorkLocationPatchRequest patch = new WorkLocationPatchRequest();
		patch.setCheckInMethod(CheckInMethod.GEOFENCE);

		assertThatThrownBy(() -> service.update(5L, patch))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void list_nonAdmin_hidesQrTokenAndOpenSessionCount() {
		WorkLocation loc = new WorkLocation();
		loc.setId(5L);
		loc.setTenant(tenant);
		loc.setName("Warehouse");
		loc.setCheckInMethod(CheckInMethod.QR);
		loc.setQrToken("secret-token");
		when(locationRepo.findAllByTenant(TENANT_ID)).thenReturn(List.of(loc));
		authenticateAs("attendance-check-in");

		List<WorkLocationDto> result = service.list(false);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getQrToken()).isNull();
		assertThat(result.get(0).getOpenSessionCount()).isNull();
		verify(sessionRepo, never()).countOpenByLocationForTenant(any());
	}

	@Test
	void list_admin_showsQrTokenAndOpenSessionCount() {
		WorkLocation loc = new WorkLocation();
		loc.setId(5L);
		loc.setTenant(tenant);
		loc.setName("Warehouse");
		loc.setCheckInMethod(CheckInMethod.QR);
		loc.setQrToken("secret-token");
		when(locationRepo.findAllByTenant(TENANT_ID)).thenReturn(List.of(loc));

		OpenSessionCount count = mock(OpenSessionCount.class);
		when(count.getLocationId()).thenReturn(5L);
		when(count.getOpenCount()).thenReturn(3L);
		when(sessionRepo.countOpenByLocationForTenant(TENANT_ID)).thenReturn(List.of(count));
		authenticateAs("location-manage");

		List<WorkLocationDto> result = service.list(false);

		assertThat(result.get(0).getQrToken()).isEqualTo("secret-token");
		assertThat(result.get(0).getOpenSessionCount()).isEqualTo(3L);
	}
}

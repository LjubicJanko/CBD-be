package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.exceptions.AttendanceDomainException;
import cbd.order_tracker.exceptions.AttendanceDomainException.Reason;
import cbd.order_tracker.model.AttendanceSession;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.User;
import cbd.order_tracker.model.WorkLocation;
import cbd.order_tracker.model.dto.request.AttendanceCheckRequest;
import cbd.order_tracker.model.dto.response.CheckOutResponseDto;
import cbd.order_tracker.model.dto.response.CurrentSessionDto;
import cbd.order_tracker.repository.AttendanceAuditLogRepository;
import cbd.order_tracker.repository.AttendanceSessionRepository;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.repository.UserRepository;
import cbd.order_tracker.repository.WorkLocationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttendanceServiceImplTest {

	private static final Long TENANT_ID = 1L;
	private static final Integer USER_ID = 42;

	private AttendanceSessionRepository sessionRepo;
	private WorkLocationRepository locationRepo;
	private AttendanceAuditLogRepository auditRepo;
	private TenantRepository tenantRepo;
	private UserRepository userRepo;

	private AttendanceServiceImpl service;
	private Tenant tenant;
	private User user;

	@BeforeEach
	void setup() {
		sessionRepo = mock(AttendanceSessionRepository.class);
		locationRepo = mock(WorkLocationRepository.class);
		auditRepo = mock(AttendanceAuditLogRepository.class);
		tenantRepo = mock(TenantRepository.class);
		userRepo = mock(UserRepository.class);
		service = new AttendanceServiceImpl(sessionRepo, locationRepo, auditRepo, tenantRepo, userRepo);

		tenant = new Tenant("CBD", "cbd");
		tenant.setId(TENANT_ID);

		user = new User("Test User", "tester", "irrelevant");
		user.setId(USER_ID);

		lenient().when(tenantRepo.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		lenient().when(sessionRepo.saveAndFlush(any(AttendanceSession.class)))
				.thenAnswer(inv -> {
					AttendanceSession s = inv.getArgument(0);
					s.setId(s.getId() == null ? 1001L : s.getId());
					return s;
				});
		lenient().when(sessionRepo.save(any(AttendanceSession.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		TenantContext.setTenantId(TENANT_ID);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
	}

	@AfterEach
	void tearDown() {
		TenantContext.clear();
		SecurityContextHolder.clearContext();
	}

	private WorkLocation location(long id, String name, double lat, double lng, int radius) {
		WorkLocation loc = new WorkLocation();
		loc.setId(id);
		loc.setTenant(tenant);
		loc.setName(name);
		loc.setLat(BigDecimal.valueOf(lat).setScale(6, RoundingMode.HALF_UP));
		loc.setLng(BigDecimal.valueOf(lng).setScale(6, RoundingMode.HALF_UP));
		loc.setRadiusM(radius);
		loc.setActive(true);
		return loc;
	}

	private AttendanceCheckRequest req(double lat, double lng, int accuracy) {
		AttendanceCheckRequest r = new AttendanceCheckRequest();
		r.setLat(BigDecimal.valueOf(lat).setScale(6, RoundingMode.HALF_UP));
		r.setLng(BigDecimal.valueOf(lng).setScale(6, RoundingMode.HALF_UP));
		r.setAccuracy(accuracy);
		return r;
	}

	// Translate a center lat/lng by `meters` north so we can pin distance exactly.
	private double[] northOf(double lat, double lng, double meters) {
		double dLat = meters / 111_320.0;
		return new double[] { lat + dLat, lng };
	}

	// === Worked examples (turn into unit tests) — table from the spec ===

	// Tolerance is the location's radiusM only; the accuracy column is fed to the request
	// purely to prove it is recorded but never affects the pass/fail decision.
	static Stream<Arguments> workedExamples() {
		return Stream.of(
				Arguments.of("allow when distance 30 <= radius 100", 30.0, 20, 100, true, null),
				Arguments.of("huge accuracy ignored: still allowed inside radius", 30.0, 5000, 100, true, null),
				Arguments.of("allow near fence edge (95 <= 100)", 95.0, 1, 100, true, null),
				Arguments.of("reject when distance 120 > radius 100", 120.0, 10, 100, false, Reason.OUT_OF_GEOFENCE),
				Arguments.of("reject far outside", 5000.0, 10, 100, false, Reason.OUT_OF_GEOFENCE)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("workedExamples")
	void geofence_workedExamples(String name, double distance, int accuracy, int radius,
								 boolean shouldAllow, Reason expectedReason) {
		double centerLat = 44.787197;
		double centerLng = 20.457273;
		WorkLocation loc = location(1L, "HQ", centerLat, centerLng, radius);
		when(locationRepo.findActiveByTenant(TENANT_ID)).thenReturn(List.of(loc));
		when(sessionRepo.findOpenForUser(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

		double[] coords = northOf(centerLat, centerLng, distance);
		AttendanceCheckRequest r = req(coords[0], coords[1], accuracy);

		if (shouldAllow) {
			CurrentSessionDto dto = service.checkIn(r, "1.1.1.1", "ua");
			assertThat(dto.getLocationId()).isEqualTo(1L);
		} else {
			assertThatThrownBy(() -> service.checkIn(r, "1.1.1.1", "ua"))
					.isInstanceOf(AttendanceDomainException.class)
					.hasFieldOrPropertyWithValue("reason", expectedReason);
		}
	}

	@Test
	void checkIn_doubleCheckIn_returnsAlreadyCheckedIn() {
		WorkLocation loc = location(1L, "HQ", 44.787197, 20.457273, 100);
		when(locationRepo.findActiveByTenant(TENANT_ID)).thenReturn(List.of(loc));

		AttendanceSession open = new AttendanceSession();
		open.setId(7L);
		when(sessionRepo.findOpenForUser(TENANT_ID, USER_ID)).thenReturn(Optional.of(open));

		assertThatThrownBy(() -> service.checkIn(req(44.787197, 20.457273, 5), "ip", "ua"))
				.isInstanceOf(AttendanceDomainException.class)
				.hasFieldOrPropertyWithValue("reason", Reason.ALREADY_CHECKED_IN);

		verify(sessionRepo, never()).saveAndFlush(any(AttendanceSession.class));
	}

	@Test
	void checkOut_withoutOpenSession_returnsNotCheckedIn() {
		when(sessionRepo.findOpenForUser(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.checkOut(req(44.787197, 20.457273, 5), "ip", "ua"))
				.isInstanceOf(AttendanceDomainException.class)
				.hasFieldOrPropertyWithValue("reason", Reason.NOT_CHECKED_IN);
	}

	@Test
	void checkIn_noActiveLocations_returnsNoActiveLocations() {
		when(locationRepo.findActiveByTenant(TENANT_ID)).thenReturn(List.of());
		when(sessionRepo.findOpenForUser(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.checkIn(req(44.787197, 20.457273, 5), "ip", "ua"))
				.isInstanceOf(AttendanceDomainException.class)
				.hasFieldOrPropertyWithValue("reason", Reason.NO_ACTIVE_LOCATIONS);
	}

	@Test
	void checkIn_multipleQualifyingLocations_picksSmallestScore() {
		double centerLat = 44.787197;
		double centerLng = 20.457273;
		// User stands at the center.
		WorkLocation closeLoc = location(1L, "Close", centerLat, centerLng, 100);
		double[] far = northOf(centerLat, centerLng, 40); // 40m north of user
		WorkLocation farLoc = location(2L, "Far", far[0], far[1], 200);

		when(locationRepo.findActiveByTenant(TENANT_ID)).thenReturn(List.of(farLoc, closeLoc));
		when(sessionRepo.findOpenForUser(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

		CurrentSessionDto dto = service.checkIn(req(centerLat, centerLng, 5), "ip", "ua");
		assertThat(dto.getLocationId()).isEqualTo(1L);
		assertThat(dto.getLocationName()).isEqualTo("Close");
	}

	@Test
	void checkIn_concurrentRace_duplicateKey_returnsAlreadyCheckedIn() {
		WorkLocation loc = location(1L, "HQ", 44.787197, 20.457273, 100);
		when(locationRepo.findActiveByTenant(TENANT_ID)).thenReturn(List.of(loc));
		when(sessionRepo.findOpenForUser(TENANT_ID, USER_ID)).thenReturn(Optional.empty());
		when(sessionRepo.saveAndFlush(any(AttendanceSession.class)))
				.thenThrow(new DataIntegrityViolationException("unique constraint"));

		assertThatThrownBy(() -> service.checkIn(req(44.787197, 20.457273, 5), "ip", "ua"))
				.isInstanceOf(AttendanceDomainException.class)
				.hasFieldOrPropertyWithValue("reason", Reason.ALREADY_CHECKED_IN);
	}

	@Test
	void checkIn_persistsIpAndUserAgentAndLocation() {
		WorkLocation loc = location(1L, "HQ", 44.787197, 20.457273, 100);
		when(locationRepo.findActiveByTenant(TENANT_ID)).thenReturn(List.of(loc));
		when(sessionRepo.findOpenForUser(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

		service.checkIn(req(44.787197, 20.457273, 12), "10.0.0.1", "Mozilla/5.0");

		ArgumentCaptor<AttendanceSession> cap = ArgumentCaptor.forClass(AttendanceSession.class);
		verify(sessionRepo).saveAndFlush(cap.capture());
		AttendanceSession saved = cap.getValue();
		assertThat(saved.getCheckInIp()).isEqualTo("10.0.0.1");
		assertThat(saved.getCheckInUserAgent()).isEqualTo("Mozilla/5.0");
		assertThat(saved.getLocation().getId()).isEqualTo(1L);
		assertThat(saved.getCheckInAt()).isNotNull();
		assertThat(saved.getTenant().getId()).isEqualTo(TENANT_ID);
		assertThat(saved.getUser().getId()).isEqualTo(USER_ID);
	}

	@Test
	void checkOut_completesSession_andReturnsDuration() {
		WorkLocation loc = location(1L, "HQ", 44.787197, 20.457273, 100);
		when(locationRepo.findActiveByTenant(TENANT_ID)).thenReturn(List.of(loc));

		AttendanceSession open = new AttendanceSession();
		open.setId(7L);
		open.setTenant(tenant);
		open.setUser(user);
		open.setLocation(loc);
		open.setCheckInAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(2));
		when(sessionRepo.findOpenForUser(TENANT_ID, USER_ID)).thenReturn(Optional.of(open));

		CheckOutResponseDto dto = service.checkOut(req(44.787197, 20.457273, 10), "ip", "ua");

		assertThat(dto.getId()).isEqualTo(7L);
		assertThat(dto.getCheckOutAt()).isNotNull();
		assertThat(dto.getDurationSeconds()).isGreaterThanOrEqualTo(7100L);
	}

	@Test
	void autoCloseStaleSessions_closesAndMarksFlag() {
		AttendanceSession stale = new AttendanceSession();
		stale.setId(9L);
		stale.setTenant(tenant);
		stale.setUser(user);
		stale.setCheckInAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(24));
		when(tenantRepo.findAll()).thenReturn(List.of(tenant));
		when(sessionRepo.findStaleOpenSessions(anyLong(), any(LocalDateTime.class))).thenReturn(List.of(stale));

		int closed = service.autoCloseStaleSessions();

		assertThat(closed).isEqualTo(1);
		assertThat(stale.getCheckOutAt()).isNotNull();
		assertThat(stale.isAutoClosed()).isTrue();
		assertThat(stale.getCheckOutLat()).isNull();
		assertThat(stale.getCheckOutLng()).isNull();
		verify(sessionRepo).saveAll(eq(List.of(stale)));
	}

	@Test
	void autoCloseStaleSessions_noStale_returnsZero() {
		when(tenantRepo.findAll()).thenReturn(List.of(tenant));
		when(sessionRepo.findStaleOpenSessions(anyLong(), any(LocalDateTime.class))).thenReturn(List.of());
		assertThat(service.autoCloseStaleSessions()).isEqualTo(0);
		verify(sessionRepo, never()).saveAll(any());
	}

	@Test
	void crossTenant_repositoryFiltering_isEnforcedViaTenantId() {
		// The service always passes TenantContext.requireTenantId() to repository lookups,
		// so a user from tenant A cannot read sessions or locations belonging to tenant B.
		// We validate that the parameter actually reaches the repository.
		when(sessionRepo.findOpenForUser(anyLong(), any(Integer.class))).thenReturn(Optional.empty());
		when(locationRepo.findActiveByTenant(anyLong())).thenReturn(List.of());

		assertThatThrownBy(() -> service.checkIn(req(44.787197, 20.457273, 5), "ip", "ua"))
				.isInstanceOf(AttendanceDomainException.class);

		verify(sessionRepo).findOpenForUser(eq(TENANT_ID), eq(USER_ID));
		verify(locationRepo).findActiveByTenant(eq(TENANT_ID));
	}
}

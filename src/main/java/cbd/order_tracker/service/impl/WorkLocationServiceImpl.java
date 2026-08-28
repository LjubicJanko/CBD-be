package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.FeatureGuard;
import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.exceptions.TenantNotFoundException;
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
import cbd.order_tracker.service.WorkLocationService;
import cbd.order_tracker.util.AttendanceMapper;
import cbd.order_tracker.util.ResourceNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkLocationServiceImpl implements WorkLocationService {

	private static final int QR_TOKEN_GENERATION_ATTEMPTS = 5;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final WorkLocationRepository locationRepo;
	private final TenantRepository tenantRepo;
	private final AttendanceSessionRepository sessionRepo;
	private final FeatureGuard featureGuard;

	private Long tenantId() {
		return TenantContext.requireTenantId();
	}

	// qrToken and open-session counts are the QR check-in security/ops surface — only
	// admins who manage locations should see them, not every check-in-capable user who
	// can list locations for name rendering.
	private boolean canManageLocations() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && auth.getAuthorities().stream()
				.anyMatch(a -> "location-manage".equals(a.getAuthority()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<WorkLocationDto> list(Boolean activeOnly) {
		Long tid = tenantId();
		List<WorkLocation> rows = Boolean.TRUE.equals(activeOnly)
				? locationRepo.findActiveByTenant(tid)
				: locationRepo.findAllByTenant(tid);

		boolean canManage = canManageLocations();
		Map<Long, Long> openCounts = canManage
				? sessionRepo.countOpenByLocationForTenant(tid).stream()
						.collect(Collectors.toMap(OpenSessionCount::getLocationId, OpenSessionCount::getOpenCount))
				: Map.of();

		return rows.stream()
				.map(l -> AttendanceMapper.toDto(l, canManage, openCounts.getOrDefault(l.getId(), 0L)))
				.toList();
	}

	@Override
	@Transactional
	public WorkLocationDto create(WorkLocationRequest req) {
		Tenant tenant = tenantRepo.findById(tenantId())
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		WorkLocation loc = new WorkLocation();
		loc.setTenant(tenant);
		loc.setName(req.getName().trim());
		loc.setCheckInMethod(req.getCheckInMethod());
		loc.setActive(req.getActive() == null || req.getActive());

		if (req.getCheckInMethod() == CheckInMethod.GEOFENCE) {
			featureGuard.requireFeature(Feature.ATTENDANCE_GEOFENCE.getKey());
			requireGeofenceFields(req.getLat(), req.getLng(), req.getRadiusM());
			loc.setLat(req.getLat());
			loc.setLng(req.getLng());
			loc.setRadiusM(req.getRadiusM());
		} else {
			loc.setQrToken(generateUniqueQrToken());
		}

		try {
			loc = locationRepo.save(loc);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("A location with this name already exists for this tenant");
		}
		return AttendanceMapper.toDto(loc, true, 0L);
	}

	@Override
	@Transactional
	public WorkLocationDto update(Long id, WorkLocationPatchRequest req) {
		WorkLocation loc = locationRepo.findByIdAndTenant(id, tenantId())
				.orElseThrow(() -> new ResourceNotFound("Location not found"));
		if (req.getName() != null) {
			String name = req.getName().trim();
			if (name.isEmpty()) {
				throw new IllegalArgumentException("name must not be blank");
			}
			loc.setName(name);
		}
		if (req.getLat() != null) loc.setLat(req.getLat());
		if (req.getLng() != null) loc.setLng(req.getLng());
		if (req.getRadiusM() != null) loc.setRadiusM(req.getRadiusM());
		if (req.getActive() != null) loc.setActive(req.getActive());
		// Gate on the caller explicitly *requesting* GEOFENCE, not on the location's resolved
		// state — an already-GEOFENCE location (e.g. one that predates this feature) must
		// stay freely editable (rename, toggle active, ...) even if the feature is disabled
		// later. Only asking to switch TO GEOFENCE is blocked.
		if (req.getCheckInMethod() == CheckInMethod.GEOFENCE) {
			featureGuard.requireFeature(Feature.ATTENDANCE_GEOFENCE.getKey());
		}
		if (req.getCheckInMethod() != null) loc.setCheckInMethod(req.getCheckInMethod());
		if (req.getCheckInMethod() == CheckInMethod.QR) {
			// Clear geofence data on switch so a later GEOFENCE switch-back can't silently
			// reuse stale coordinates the admin never resupplied.
			loc.setLat(null);
			loc.setLng(null);
			loc.setRadiusM(null);
		}

		if (loc.getCheckInMethod() == CheckInMethod.GEOFENCE) {
			requireGeofenceFields(loc.getLat(), loc.getLng(), loc.getRadiusM());
		} else if (loc.getQrToken() == null) {
			// First time this location becomes QR-capable — mint its token. Kept forever
			// after that (regeneration is a deliberate future admin action, not automatic).
			loc.setQrToken(generateUniqueQrToken());
		}

		try {
			loc = locationRepo.save(loc);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("A location with this name already exists for this tenant");
		}
		Long locId = loc.getId();
		long openCount = sessionRepo.countOpenByLocationForTenant(tenantId()).stream()
				.filter(c -> c.getLocationId().equals(locId))
				.findFirst()
				.map(OpenSessionCount::getOpenCount)
				.orElse(0L);
		return AttendanceMapper.toDto(loc, true, openCount);
	}

	private void requireGeofenceFields(java.math.BigDecimal lat, java.math.BigDecimal lng, Integer radiusM) {
		if (lat == null || lng == null || radiusM == null) {
			throw new IllegalArgumentException("lat, lng and radiusM are required for GEOFENCE locations");
		}
	}

	private String generateUniqueQrToken() {
		for (int i = 0; i < QR_TOKEN_GENERATION_ATTEMPTS; i++) {
			byte[] bytes = new byte[16];
			SECURE_RANDOM.nextBytes(bytes);
			String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
			if (!locationRepo.existsByQrToken(token)) {
				return token;
			}
		}
		throw new IllegalStateException("Failed to generate a unique QR token after " + QR_TOKEN_GENERATION_ATTEMPTS + " attempts");
	}

	@Override
	@Transactional
	public void delete(Long id) {
		WorkLocation loc = locationRepo.findByIdAndTenant(id, tenantId())
				.orElseThrow(() -> new ResourceNotFound("Location not found"));
		long sessions = locationRepo.countSessionsForLocation(id);
		if (sessions == 0) {
			locationRepo.delete(loc);
		} else {
			loc.setActive(false);
			locationRepo.save(loc);
		}
	}
}

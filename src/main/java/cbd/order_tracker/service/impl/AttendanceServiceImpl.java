package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.exceptions.AttendanceDomainException;
import cbd.order_tracker.exceptions.AttendanceDomainException.Reason;
import cbd.order_tracker.exceptions.TenantNotFoundException;
import cbd.order_tracker.exceptions.UserNotFoundException;
import cbd.order_tracker.model.AttendanceAuditLog;
import cbd.order_tracker.model.AttendanceSession;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.User;
import cbd.order_tracker.model.WorkLocation;
import cbd.order_tracker.model.dto.PageableResponse;
import cbd.order_tracker.model.dto.request.AttendanceAdminCreateRequest;
import cbd.order_tracker.model.dto.request.AttendanceAdminPatchRequest;
import cbd.order_tracker.model.dto.request.AttendanceCheckRequest;
import cbd.order_tracker.model.dto.response.AttendanceSessionDto;
import cbd.order_tracker.model.dto.response.CheckOutResponseDto;
import cbd.order_tracker.model.dto.response.CurrentSessionDto;
import cbd.order_tracker.repository.AttendanceAuditLogRepository;
import cbd.order_tracker.repository.AttendanceSessionRepository;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.repository.UserRepository;
import cbd.order_tracker.repository.WorkLocationRepository;
import cbd.order_tracker.service.AttendanceService;
import cbd.order_tracker.util.AttendanceMapper;
import cbd.order_tracker.util.ResourceNotFound;
import cbd.order_tracker.util.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class AttendanceServiceImpl implements AttendanceService {

	private static final Logger log = LoggerFactory.getLogger(AttendanceServiceImpl.class);

	static final int AUTO_CLOSE_HOURS = 16;
	private static final double EARTH_RADIUS_M = 6_371_000.0;

	private final AttendanceSessionRepository sessionRepo;
	private final WorkLocationRepository locationRepo;
	private final AttendanceAuditLogRepository auditRepo;
	private final TenantRepository tenantRepo;
	private final UserRepository userRepo;

	public AttendanceServiceImpl(
			AttendanceSessionRepository sessionRepo,
			WorkLocationRepository locationRepo,
			AttendanceAuditLogRepository auditRepo,
			TenantRepository tenantRepo,
			UserRepository userRepo
	) {
		this.sessionRepo = sessionRepo;
		this.locationRepo = locationRepo;
		this.auditRepo = auditRepo;
		this.tenantRepo = tenantRepo;
		this.userRepo = userRepo;
	}

	private Long tenantId() {
		return TenantContext.requireTenantId();
	}

	private User currentUser() {
		User u = UserUtil.getCurrentUser();
		if (u == null) {
			throw new ResourceNotFound("No authenticated user");
		}
		return u;
	}

	// Geofence match against active locations for the current tenant. Returns the active
	// location with the smallest distance that satisfies distance <= radius. Throws the
	// appropriate AttendanceDomainException otherwise. The tolerance lives entirely in
	// WorkLocation.radiusM; the client-reported accuracy is recorded on the session but never
	// affects the pass/fail decision (a client-sent tolerance would be spoofable).
	WorkLocation matchGeofence(Long tid, BigDecimal lat, BigDecimal lng) {
		return matchGeofence(tid, lat, lng, null);
	}

	// Variant that includes alwaysInclude in the candidate set even if it is inactive.
	// Used for check-out to prevent locking out a user whose check-in location was
	// deactivated mid-shift.
	WorkLocation matchGeofence(Long tid, BigDecimal lat, BigDecimal lng, WorkLocation alwaysInclude) {
		List<WorkLocation> candidates = new ArrayList<>(locationRepo.findActiveByTenant(tid));
		if (alwaysInclude != null && candidates.stream().noneMatch(l -> l.getId().equals(alwaysInclude.getId()))) {
			candidates.add(alwaysInclude);
		}
		if (candidates.isEmpty()) {
			throw new AttendanceDomainException(Reason.NO_ACTIVE_LOCATIONS,
					"No active work locations configured for this tenant");
		}

		WorkLocation best = null;
		double bestScore = Double.MAX_VALUE;
		double lat1 = lat.doubleValue();
		double lng1 = lng.doubleValue();
		for (WorkLocation loc : candidates) {
			double distance = haversineMeters(
					lat1, lng1,
					loc.getLat().doubleValue(), loc.getLng().doubleValue()
			);
			if (distance <= loc.getRadiusM() && distance < bestScore) {
				bestScore = distance;
				best = loc;
			}
		}
		if (best == null) {
			throw new AttendanceDomainException(Reason.OUT_OF_GEOFENCE,
					"You are not within any active work location");
		}
		return best;
	}

	static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
		double phi1 = Math.toRadians(lat1);
		double phi2 = Math.toRadians(lat2);
		double dPhi = Math.toRadians(lat2 - lat1);
		double dLambda = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
				+ Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_M * c;
	}

	@Override
	@Transactional
	public CurrentSessionDto checkIn(AttendanceCheckRequest req, String ip, String userAgent) {
		Long tid = tenantId();
		User user = currentUser();

		// Order matters — see the prompt's "Check order on check-in / check-out".
		// 1) Body validation handled by @Valid in the controller.
		// 2) Existing-session conflict.
		if (sessionRepo.findOpenForUser(tid, user.getId()).isPresent()) {
			throw new AttendanceDomainException(Reason.ALREADY_CHECKED_IN,
					"You already have an open attendance session");
		}

		// 3) Geofence match. Tolerance is the location's radiusM; client accuracy is recorded only.
		WorkLocation loc = matchGeofence(tid, req.getLat(), req.getLng());

		Tenant tenant = tenantRepo.findById(tid)
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

		AttendanceSession s = new AttendanceSession();
		s.setTenant(tenant);
		s.setUser(user);
		s.setLocation(loc);
		s.setCheckInAt(nowUtc());
		s.setCheckInLat(req.getLat());
		s.setCheckInLng(req.getLng());
		s.setCheckInAccuracyM(req.getAccuracy());
		s.setCheckInIp(ip);
		s.setCheckInUserAgent(truncate(userAgent, 500));

		try {
			s = sessionRepo.saveAndFlush(s);
		} catch (DataIntegrityViolationException e) {
			// Defensive: another request raced us through the open-session unique index.
			throw new AttendanceDomainException(Reason.ALREADY_CHECKED_IN,
					"You already have an open attendance session");
		}
		return AttendanceMapper.toCurrentSessionDto(s);
	}

	@Override
	@Transactional
	public CheckOutResponseDto checkOut(AttendanceCheckRequest req, String ip, String userAgent) {
		Long tid = tenantId();
		User user = currentUser();

		AttendanceSession s = sessionRepo.findOpenForUser(tid, user.getId())
				.orElseThrow(() -> new AttendanceDomainException(Reason.NOT_CHECKED_IN,
						"You do not have an open attendance session"));

		// Geofence check for check-out — session's check-in location is always included
		// so the user isn't locked out if that location was deactivated mid-shift.
		matchGeofence(tid, req.getLat(), req.getLng(), s.getLocation());

		s.setCheckOutAt(nowUtc());
		s.setCheckOutLat(req.getLat());
		s.setCheckOutLng(req.getLng());
		s.setCheckOutAccuracyM(req.getAccuracy());
		s.setCheckOutIp(ip);
		s.setCheckOutUserAgent(truncate(userAgent, 500));
		s = sessionRepo.save(s);
		return AttendanceMapper.toCheckOutResponseDto(s);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CurrentSessionDto> currentForUser() {
		// "My current session" probe: a caller without a tenant context (e.g. a superadmin
		// who hasn't selected a tenant via X-Tenant-Id) simply has no open session, so return
		// empty (-> 204) rather than the 403 that requireTenantId() would raise.
		Long tid = TenantContext.getTenantId();
		if (tid == null) {
			return Optional.empty();
		}
		User user = currentUser();
		return sessionRepo.findOpenForUser(tid, user.getId())
				.map(AttendanceMapper::toCurrentSessionDto);
	}

	@Override
	@Transactional(readOnly = true)
	public PageableResponse<AttendanceSessionDto> search(
			LocalDate date, LocalDate from, LocalDate to,
			Integer userId, Long locationId, boolean openOnly,
			int page, int size, String sort
	) {
		Long tid = tenantId();
		int pageSize = Math.min(Math.max(size, 1), 100);

		LocalDateTime fromTs;
		LocalDateTime toTs;
		if (date != null) {
			fromTs = date.atStartOfDay();
			toTs = date.plusDays(1).atStartOfDay();
		} else {
			fromTs = from == null ? null : from.atStartOfDay();
			toTs = to == null ? null : to.plusDays(1).atStartOfDay();
		}

		Sort sortObj = parseSort(sort);
		Pageable pageable = PageRequest.of(Math.max(page, 0), pageSize, sortObj);
		Page<AttendanceSession> result = sessionRepo.search(
				tid, fromTs, toTs, userId, locationId, openOnly, pageable
		);
		List<AttendanceSessionDto> data = result.stream().map(AttendanceMapper::toSessionDto).toList();
		return new PageableResponse<>(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				result.getTotalPages(),
				result.getTotalElements(),
				data
		);
	}

	private Sort parseSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return Sort.by(Sort.Direction.DESC, "checkInAt");
		}
		String[] parts = sort.split(",");
		String field = parts[0].trim();
		Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
				? Sort.Direction.ASC : Sort.Direction.DESC;
		// Whitelist the fields a client may sort on.
		String mapped = switch (field) {
			case "checkInAt" -> "checkInAt";
			case "checkOutAt" -> "checkOutAt";
			case "id" -> "id";
			default -> "checkInAt";
		};
		return Sort.by(dir, mapped);
	}

	@Override
	@Transactional
	public AttendanceSessionDto adminCreate(AttendanceAdminCreateRequest req) {
		Long tid = tenantId();
		Tenant tenant = tenantRepo.findById(tid)
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		User target = userRepo.findById(req.getUserId())
				.orElseThrow(() -> new UserNotFoundException("User not found"));
		if (target.getTenant() == null || !Objects.equals(target.getTenant().getId(), tid)) {
			throw new ResourceNotFound("User not found");
		}
		WorkLocation loc = locationRepo.findByIdAndTenant(req.getLocationId(), tid)
				.orElseThrow(() -> new ResourceNotFound("Location not found"));

		if (req.getCheckOutAt() != null && !req.getCheckOutAt().isAfter(req.getCheckInAt())) {
			throw new IllegalArgumentException("checkOutAt must be after checkInAt");
		}

		AttendanceSession s = new AttendanceSession();
		s.setTenant(tenant);
		s.setUser(target);
		s.setLocation(loc);
		s.setCheckInAt(req.getCheckInAt());
		s.setCheckInLat(loc.getLat());
		s.setCheckInLng(loc.getLng());
		s.setCheckInAccuracyM(0);
		s.setCheckOutAt(req.getCheckOutAt());
		if (req.getCheckOutAt() != null) {
			s.setCheckOutLat(loc.getLat());
			s.setCheckOutLng(loc.getLng());
			s.setCheckOutAccuracyM(0);
		}
		s.setAutoClosed(false);
		s.setNotes(req.getNotes());

		try {
			s = sessionRepo.saveAndFlush(s);
		} catch (DataIntegrityViolationException e) {
			// Admin tried to create an open session while user already has one.
			throw new AttendanceDomainException(Reason.ALREADY_CHECKED_IN,
					"User already has an open attendance session");
		}
		audit(s, "CREATE", null, null, null);
		return AttendanceMapper.toSessionDto(s);
	}

	@Override
	@Transactional
	public AttendanceSessionDto adminPatch(Long id, AttendanceAdminPatchRequest req) {
		Long tid = tenantId();
		AttendanceSession s = sessionRepo.findByIdAndTenant(id, tid)
				.orElseThrow(() -> new ResourceNotFound("Session not found"));

		LocalDateTime newIn = req.getCheckInAt() != null ? req.getCheckInAt() : s.getCheckInAt();
		LocalDateTime newOut = req.getCheckOutAt() != null ? req.getCheckOutAt() : s.getCheckOutAt();
		if (newOut != null && !newOut.isAfter(newIn)) {
			throw new IllegalArgumentException("checkOutAt must be after checkInAt");
		}

		if (req.getCheckInAt() != null && !Objects.equals(req.getCheckInAt(), s.getCheckInAt())) {
			audit(s, "EDIT", "checkInAt", String.valueOf(s.getCheckInAt()), String.valueOf(req.getCheckInAt()));
			s.setCheckInAt(req.getCheckInAt());
		}
		if (req.getCheckOutAt() != null && !Objects.equals(req.getCheckOutAt(), s.getCheckOutAt())) {
			audit(s, "EDIT", "checkOutAt", String.valueOf(s.getCheckOutAt()), String.valueOf(req.getCheckOutAt()));
			s.setCheckOutAt(req.getCheckOutAt());
		}
		if (req.getNotes() != null && !Objects.equals(req.getNotes(), s.getNotes())) {
			audit(s, "EDIT", "notes", s.getNotes(), req.getNotes());
			s.setNotes(req.getNotes());
		}
		try {
			s = sessionRepo.saveAndFlush(s);
		} catch (DataIntegrityViolationException e) {
			// Reopening a session for a user who already has another open one.
			throw new AttendanceDomainException(Reason.ALREADY_CHECKED_IN,
					"User already has another open attendance session");
		}
		return AttendanceMapper.toSessionDto(s);
	}

	@Override
	@Transactional
	public int autoCloseStaleSessions() {
		LocalDateTime threshold = nowUtc().minusHours(AUTO_CLOSE_HOURS);
		LocalDateTime closedAt = nowUtc();
		int total = 0;
		for (Tenant tenant : tenantRepo.findAll()) {
			List<AttendanceSession> stale = sessionRepo.findStaleOpenSessions(tenant.getId(), threshold);
			for (AttendanceSession s : stale) {
				s.setCheckOutAt(closedAt);
				s.setAutoClosed(true);
				// check_out_* location fields remain NULL — admins can edit afterwards.
			}
			if (!stale.isEmpty()) {
				sessionRepo.saveAll(stale);
				stale.forEach(s -> audit(s, "AUTO_CLOSE", "checkOutAt", null, String.valueOf(closedAt)));
				log.info("Auto-closed {} stale attendance sessions for tenant {} older than {} hours",
						stale.size(), tenant.getId(), AUTO_CLOSE_HOURS);
			}
			total += stale.size();
		}
		return total;
	}

	private void audit(AttendanceSession s, String action, String field, String oldV, String newV) {
		User actor = UserUtil.getCurrentUser();
		AttendanceAuditLog row = new AttendanceAuditLog();
		row.setTenantId(s.getTenant().getId());
		row.setSessionId(s.getId());
		row.setActorUserId(actor != null ? actor.getId() : null);
		row.setActorUsername(actor != null ? actor.getUsername() : "system");
		row.setAction(action);
		row.setFieldName(field);
		row.setOldValue(truncate(oldV, 500));
		row.setNewValue(truncate(newV, 500));
		auditRepo.save(row);
	}

	private static String truncate(String s, int max) {
		if (s == null) return null;
		return s.length() <= max ? s : s.substring(0, max);
	}

	private static LocalDateTime nowUtc() {
		return LocalDateTime.now(ZoneOffset.UTC);
	}
}

package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.exceptions.TenantNotFoundException;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.WorkLocation;
import cbd.order_tracker.model.dto.request.WorkLocationPatchRequest;
import cbd.order_tracker.model.dto.request.WorkLocationRequest;
import cbd.order_tracker.model.dto.response.WorkLocationDto;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.repository.WorkLocationRepository;
import cbd.order_tracker.service.WorkLocationService;
import cbd.order_tracker.util.AttendanceMapper;
import cbd.order_tracker.util.ResourceNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkLocationServiceImpl implements WorkLocationService {

	private final WorkLocationRepository locationRepo;
	private final TenantRepository tenantRepo;

	private Long tenantId() {
		return TenantContext.requireTenantId();
	}

	@Override
	@Transactional(readOnly = true)
	public List<WorkLocationDto> list(Boolean activeOnly) {
		Long tid = tenantId();
		List<WorkLocation> rows = Boolean.TRUE.equals(activeOnly)
				? locationRepo.findActiveByTenant(tid)
				: locationRepo.findAllByTenant(tid);
		return rows.stream().map(AttendanceMapper::toDto).toList();
	}

	@Override
	@Transactional
	public WorkLocationDto create(WorkLocationRequest req) {
		Tenant tenant = tenantRepo.findById(tenantId())
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		WorkLocation loc = new WorkLocation();
		loc.setTenant(tenant);
		loc.setName(req.getName().trim());
		loc.setLat(req.getLat());
		loc.setLng(req.getLng());
		loc.setRadiusM(req.getRadiusM());
		loc.setActive(req.getActive() == null || req.getActive());
		try {
			loc = locationRepo.save(loc);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("A location with this name already exists for this tenant");
		}
		return AttendanceMapper.toDto(loc);
	}

	@Override
	@Transactional
	public WorkLocationDto update(Long id, WorkLocationPatchRequest req) {
		WorkLocation loc = locationRepo.findByIdAndTenant(id, tenantId())
				.orElseThrow(() -> new ResourceNotFound("Location not found"));
		if (req.getName() != null) loc.setName(req.getName().trim());
		if (req.getLat() != null) loc.setLat(req.getLat());
		if (req.getLng() != null) loc.setLng(req.getLng());
		if (req.getRadiusM() != null) loc.setRadiusM(req.getRadiusM());
		if (req.getActive() != null) loc.setActive(req.getActive());
		try {
			loc = locationRepo.save(loc);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("A location with this name already exists for this tenant");
		}
		return AttendanceMapper.toDto(loc);
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

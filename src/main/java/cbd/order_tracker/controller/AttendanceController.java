package cbd.order_tracker.controller;

import cbd.order_tracker.model.dto.PageableResponse;
import cbd.order_tracker.model.dto.request.AttendanceAdminCreateRequest;
import cbd.order_tracker.model.dto.request.AttendanceAdminPatchRequest;
import cbd.order_tracker.model.dto.request.AttendanceCheckRequest;
import cbd.order_tracker.model.dto.request.AttendanceScanRequest;
import cbd.order_tracker.model.dto.response.AttendanceSessionDto;
import cbd.order_tracker.model.dto.response.CheckOutResponseDto;
import cbd.order_tracker.model.dto.response.CurrentSessionDto;
import cbd.order_tracker.model.dto.response.ScanLocationDto;
import cbd.order_tracker.model.dto.response.ScanResultDto;
import cbd.order_tracker.service.AttendanceService;
import cbd.order_tracker.util.RequestMetadataUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

	private final AttendanceService attendanceService;

	// Public: QR landing page, resolved before login. Path shape (two segments after
	// /scan/) is what keeps this out of FeatureEnforcementInterceptor's authenticated gate
	// and out of Spring Security's auth requirement — see both configs before changing this.
	// Tenant resolution and the feature check both happen inside resolveScanLocation, so they
	// aren't duplicated here.
	@GetMapping("/scan/{tenantSlug}/{token}")
	public ResponseEntity<ScanLocationDto> scanInfo(@PathVariable String tenantSlug, @PathVariable String token) {
		return ResponseEntity.ok(attendanceService.resolveScanLocation(tenantSlug, token));
	}

	@PreAuthorize("hasAuthority('attendance-check-in')")
	@PostMapping("/scan/{token}")
	public ResponseEntity<ScanResultDto> scan(
			@PathVariable String token,
			@Valid @RequestBody AttendanceScanRequest req,
			HttpServletRequest httpReq
	) {
		ScanResultDto dto = attendanceService.scan(
				token,
				req,
				RequestMetadataUtil.clientIp(httpReq),
				RequestMetadataUtil.userAgent(httpReq)
		);
		return ResponseEntity.ok(dto);
	}

	@PreAuthorize("hasAuthority('attendance-check-in')")
	@PostMapping("/check-in")
	public ResponseEntity<CurrentSessionDto> checkIn(
			@Valid @RequestBody AttendanceCheckRequest req,
			HttpServletRequest httpReq
	) {
		CurrentSessionDto dto = attendanceService.checkIn(
				req,
				RequestMetadataUtil.clientIp(httpReq),
				RequestMetadataUtil.userAgent(httpReq)
		);
		return ResponseEntity.ok(dto);
	}

	@PreAuthorize("hasAuthority('attendance-check-in')")
	@PostMapping("/check-out")
	public ResponseEntity<CheckOutResponseDto> checkOut(
			@Valid @RequestBody AttendanceCheckRequest req,
			HttpServletRequest httpReq
	) {
		CheckOutResponseDto dto = attendanceService.checkOut(
				req,
				RequestMetadataUtil.clientIp(httpReq),
				RequestMetadataUtil.userAgent(httpReq)
		);
		return ResponseEntity.ok(dto);
	}

	@PreAuthorize("hasAuthority('attendance-check-in')")
	@GetMapping("/me/current")
	public ResponseEntity<CurrentSessionDto> currentForMe() {
		return attendanceService.currentForUser()
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PreAuthorize("hasAuthority('attendance-view-all')")
	@GetMapping
	public ResponseEntity<PageableResponse<AttendanceSessionDto>> search(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Integer userId,
			@RequestParam(required = false) Long locationId,
			@RequestParam(required = false, defaultValue = "false") boolean openOnly,
			@RequestParam(required = false, defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "20") int size,
			@RequestParam(required = false, defaultValue = "checkInAt,desc") String sort
	) {
		return ResponseEntity.ok(
				attendanceService.search(date, from, to, userId, locationId, openOnly, page, size, sort)
		);
	}

	@PreAuthorize("hasAuthority('attendance-edit')")
	@PostMapping
	public ResponseEntity<AttendanceSessionDto> adminCreate(
			@Valid @RequestBody AttendanceAdminCreateRequest req
	) {
		return ResponseEntity.ok(attendanceService.adminCreate(req));
	}

	@PreAuthorize("hasAuthority('attendance-edit')")
	@PatchMapping("/{id}")
	public ResponseEntity<AttendanceSessionDto> adminPatch(
			@PathVariable Long id,
			@Valid @RequestBody AttendanceAdminPatchRequest req
	) {
		return ResponseEntity.ok(attendanceService.adminPatch(id, req));
	}
}

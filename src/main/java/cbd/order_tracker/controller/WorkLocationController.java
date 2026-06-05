package cbd.order_tracker.controller;

import cbd.order_tracker.model.dto.request.WorkLocationPatchRequest;
import cbd.order_tracker.model.dto.request.WorkLocationRequest;
import cbd.order_tracker.model.dto.response.WorkLocationDto;
import cbd.order_tracker.service.WorkLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class WorkLocationController {

	private final WorkLocationService service;

	// Readable by users who can check in (so the FE can render location names),
	// as well as by admins who manage locations.
	@PreAuthorize("hasAuthority('location-manage') or hasAuthority('attendance-check-in') or hasAuthority('attendance-view-all')")
	@GetMapping
	public ResponseEntity<List<WorkLocationDto>> list(
			@RequestParam(required = false) Boolean activeOnly
	) {
		return ResponseEntity.ok(service.list(activeOnly));
	}

	@PreAuthorize("hasAuthority('location-manage')")
	@PostMapping
	public ResponseEntity<WorkLocationDto> create(@Valid @RequestBody WorkLocationRequest req) {
		return ResponseEntity.ok(service.create(req));
	}

	@PreAuthorize("hasAuthority('location-manage')")
	@PatchMapping("/{id}")
	public ResponseEntity<WorkLocationDto> patch(
			@PathVariable Long id,
			@Valid @RequestBody WorkLocationPatchRequest req
	) {
		return ResponseEntity.ok(service.update(id, req));
	}

	@PreAuthorize("hasAuthority('location-manage')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}

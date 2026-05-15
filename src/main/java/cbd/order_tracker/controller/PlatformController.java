package cbd.order_tracker.controller;

import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.RegisterUserDto;
import cbd.order_tracker.model.dto.request.CreateTenantReqDto;
import cbd.order_tracker.model.dto.response.TenantResDto;
import cbd.order_tracker.service.PlatformService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/platform")
@PreAuthorize("hasRole('SUPERADMIN')")
@RequiredArgsConstructor
public class PlatformController {

	private final PlatformService platformService;

	@GetMapping("/tenants")
	public ResponseEntity<List<TenantResDto>> getAllTenants() {
		return ResponseEntity.ok(platformService.getAllTenants());
	}

	@GetMapping("/tenants/{id}")
	public ResponseEntity<TenantResDto> getTenant(@PathVariable Long id) {
		return ResponseEntity.ok(platformService.getTenantById(id));
	}

	@PostMapping("/tenants")
	public ResponseEntity<TenantResDto> createTenant(@Valid @RequestBody CreateTenantReqDto dto) {
		return ResponseEntity.ok(platformService.createTenant(dto));
	}

	@PutMapping("/tenants/{id}")
	public ResponseEntity<TenantResDto> updateTenant(@PathVariable Long id, @Valid @RequestBody CreateTenantReqDto dto) {
		return ResponseEntity.ok(platformService.updateTenant(id, dto));
	}

	@DeleteMapping("/tenants/{id}")
	public ResponseEntity<Void> deactivateTenant(@PathVariable Long id) {
		platformService.deactivateTenant(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/tenants/{tenantId}/users")
	public ResponseEntity<User> createUserForTenant(@PathVariable Long tenantId, @RequestBody RegisterUserDto dto) {
		User user = platformService.createUserForTenant(tenantId, dto);
		return ResponseEntity.ok(user);
	}

	@PostMapping(value = "/tenants/{id}/logo", consumes = "multipart/form-data")
	public ResponseEntity<TenantResDto> uploadLogo(@PathVariable Long id, @RequestParam("logo") MultipartFile logo) {
		return ResponseEntity.ok(platformService.uploadLogo(id, logo));
	}

	@DeleteMapping("/tenants/{id}/logo")
	public ResponseEntity<Void> deleteLogo(@PathVariable Long id) {
		platformService.deleteLogo(id);
		return ResponseEntity.ok().build();
	}
}

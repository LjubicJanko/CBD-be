package cbd.order_tracker.controller;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.exceptions.OrderNotFoundException;
import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.ChangePasswordDto;
import cbd.order_tracker.model.dto.RegisterUserDto;
import cbd.order_tracker.model.dto.UserDto;
import cbd.order_tracker.model.dto.request.UpdateOwnTenantReqDto;
import cbd.order_tracker.model.dto.response.TenantResDto;
import cbd.order_tracker.service.AuthenticationService;
import cbd.order_tracker.service.PlatformService;
import cbd.order_tracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/api/profile")
@RestController
public class ProfileController {

	private final UserService userService;

	private final AuthenticationService authenticationService;

	private final PlatformService platformService;

	public ProfileController(UserService userService, AuthenticationService authenticationService, PlatformService platformService) {
		this.userService = userService;
		this.authenticationService = authenticationService;
		this.platformService = platformService;
	}

	@GetMapping("/tenant")
	@PreAuthorize("hasRole('company_admin')")
	public ResponseEntity<TenantResDto> getOwnTenant() {
		return ResponseEntity.ok(platformService.getTenantById(ownTenantId()));
	}

	@PutMapping("/tenant")
	@PreAuthorize("hasRole('company_admin')")
	public ResponseEntity<TenantResDto> updateOwnTenant(@Valid @RequestBody UpdateOwnTenantReqDto dto) {
		return ResponseEntity.ok(platformService.updateOwnTenant(ownTenantId(), dto));
	}

	@PostMapping(value = "/tenant/logo", consumes = "multipart/form-data")
	@PreAuthorize("hasRole('company_admin')")
	public ResponseEntity<TenantResDto> uploadOwnLogo(@RequestParam("logo") MultipartFile logo) {
		return ResponseEntity.ok(platformService.uploadLogo(ownTenantId(), logo));
	}

	@DeleteMapping("/tenant/logo")
	@PreAuthorize("hasRole('company_admin')")
	public ResponseEntity<Void> deleteOwnLogo() {
		platformService.deleteLogo(ownTenantId());
		return ResponseEntity.ok().build();
	}

	// Resolve the caller's tenant strictly from the JWT-derived context. The auth filter
	// only populates X-Tenant-Id for superadmins, so for a company_admin this is purely the
	// token's tenant claim. We reject superadmins outright (they have no "own" tenant — they
	// use /api/platform) so X-Tenant-Id can never steer these self-service routes. 403 when
	// there is no tenant context.
	private Long ownTenantId() {
		if (TenantContext.isSuperadmin()) {
			throw new AccessDeniedException("Superadmin has no own tenant; use /api/platform");
		}
		Long tenantId = TenantContext.getTenantId();
		if (tenantId == null) {
			throw new AccessDeniedException("No tenant context");
		}
		return tenantId;
	}


	@GetMapping("/admin/allUsers")
	public ResponseEntity<List<UserDto>> allUsers() {

		try {
			List<UserDto> users = userService.allUsers();
			return ResponseEntity.ok(users);
		} catch (OrderNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}
	}

	@PostMapping("/signup")
	@PreAuthorize("hasRole('SUPERADMIN') or hasRole('company_admin')")
	public ResponseEntity<User> register(@RequestBody RegisterUserDto registerUserDto) {
		User registeredUser = authenticationService.signup(registerUserDto);

		return ResponseEntity.ok(registeredUser);
	}


	@PutMapping("/change-password")
	public ResponseEntity<UserDto> changePassword(@RequestBody ChangePasswordDto changePasswordDto) {
		return ResponseEntity.ok(authenticationService.changePassword(changePasswordDto));
	}
}

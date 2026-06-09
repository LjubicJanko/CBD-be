package cbd.order_tracker.controller;

import cbd.order_tracker.model.*;
import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.LoginUserDto;
import cbd.order_tracker.model.enums.Feature;
import cbd.order_tracker.repository.PrivilegeRepository;
import cbd.order_tracker.service.AuthenticationService;
import cbd.order_tracker.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequestMapping("/api/auth")
@RestController
public class AuthenticationController {
	private final JwtService jwtService;

	private final AuthenticationService authenticationService;

	private final PrivilegeRepository privilegeRepository;

	public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService, PrivilegeRepository privilegeRepository) {
		this.jwtService = jwtService;
		this.authenticationService = authenticationService;
		this.privilegeRepository = privilegeRepository;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto) {
		User authenticatedUser = authenticationService.authenticate(loginUserDto);

		Map<String, Object> extraClaims = new HashMap<>();
		Tenant tenant = authenticatedUser.getTenant();
		extraClaims.put("tenantId", tenant != null ? tenant.getId() : null);
		extraClaims.put("superadmin", authenticatedUser.isSuperadmin());

		String jwtToken = jwtService.generateToken(extraClaims, authenticatedUser);
		LoginResponse loginResponse = new LoginResponse(
				authenticatedUser.getId(),
				jwtToken,
				jwtService.getExpirationTime(),
				authenticatedUser.getRoles(),
				authenticatedUser.getFullName(),
				authenticatedUser.getUsername(),
				tenant != null ? tenant.getId() : null,
				tenant != null ? tenant.getSlug() : null,
				authenticatedUser.isSuperadmin()
		);

		if (tenant != null && tenant.getLogoBytes() != null && tenant.getLogoBytes().length > 0) {
			loginResponse.setTenantLogoUrl("/api/public/tenants/" + tenant.getSlug() + "/logo");
		}

		loginResponse.setFeatures(tenant != null ? new HashSet<>(tenant.getFeatures()) : new HashSet<>());

		// Response-shaping only: superadmin has no users_roles row in DB.
		// We project as company_admin + all privileges so the FE's privilege gates
		// light up uniformly. The actual granted authority is ROLE_SUPERADMIN.
		if (authenticatedUser.isSuperadmin()) {
			loginResponse.setRoles(Set.of("company_admin"));
			Set<String> allPrivileges = privilegeRepository.findAll().stream()
					.map(Privilege::getName)
					.collect(Collectors.toSet());
			loginResponse.setPrivileges(allPrivileges);
			// Superadmin bypasses feature gates; expose all modules so the FE shows them.
			loginResponse.setFeatures(new HashSet<>(Feature.KEYS));
		}

		return ResponseEntity.ok(loginResponse);
	}
}

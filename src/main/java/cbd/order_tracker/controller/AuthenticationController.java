package cbd.order_tracker.controller;

import cbd.order_tracker.model.*;
import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.LoginUserDto;
import cbd.order_tracker.model.dto.RegisterUserDto;
import cbd.order_tracker.model.dto.response.AuthResult;
import cbd.order_tracker.service.AuthenticationService;
import cbd.order_tracker.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/auth")
@RestController
public class AuthenticationController {
	private final JwtService jwtService;

	private final AuthenticationService authenticationService;

	public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
		this.jwtService = jwtService;
		this.authenticationService = authenticationService;
	}

	@PostMapping("/signup")
	public ResponseEntity<User> register(@RequestBody RegisterUserDto registerUserDto) {
		User registeredUser = authenticationService.signup(registerUserDto);

		return ResponseEntity.ok(registeredUser);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto) {
		AuthResult authResult = authenticationService.authenticate(loginUserDto);
		User user = authResult.user();
		List<Long> companyIds = authResult.companyIds();

		Map<String, Object> claims = new HashMap<>();
		claims.put("companyIds", companyIds);
		claims.put("roles", user.getRoles().stream().map(Role::getName).toList());

		String jwtToken = jwtService.generateToken(claims, user);

		LoginResponse res = new LoginResponse(
				user.getId(),
				jwtToken,
				jwtService.getExpirationTime(),
				user.getRoles(),
				user.getFullName(),
				user.getUsername(),
				user.getCompanies()
		);
		return ResponseEntity.ok(res);
	}


}

package cbd.order_tracker.controller;

import cbd.order_tracker.exceptions.OrderNotFoundException;
import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.ChangePasswordDto;
import cbd.order_tracker.model.dto.RegisterUserDto;
import cbd.order_tracker.model.dto.UserDto;
import cbd.order_tracker.service.AuthenticationService;
import cbd.order_tracker.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/profile")
@RestController
public class ProfileController {

	private final UserService userService;

	private final AuthenticationService authenticationService;

	public ProfileController(UserService userService, AuthenticationService authenticationService) {
		this.userService = userService;
		this.authenticationService = authenticationService;
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
	public ResponseEntity<User> register(@RequestBody RegisterUserDto registerUserDto) {
		User registeredUser = authenticationService.signup(registerUserDto);

		return ResponseEntity.ok(registeredUser);
	}


	@PutMapping("/change-password")
	public ResponseEntity<UserDto> changePassword(@RequestBody ChangePasswordDto changePasswordDto) {
		return ResponseEntity.ok(authenticationService.changePassword(changePasswordDto));
	}
}

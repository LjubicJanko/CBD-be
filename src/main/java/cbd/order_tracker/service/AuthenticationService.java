package cbd.order_tracker.service;

import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.ChangePasswordDto;
import cbd.order_tracker.model.dto.LoginUserDto;
import cbd.order_tracker.model.dto.RegisterUserDto;
import cbd.order_tracker.model.dto.UserDto;
import cbd.order_tracker.model.dto.response.AuthResult;
import cbd.order_tracker.repository.RolesRepository;
import cbd.order_tracker.repository.UserRepository;
import cbd.order_tracker.util.UserMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthenticationService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final RolesRepository rolesRepository;

	public AuthenticationService(
			UserRepository userRepository,
			AuthenticationManager authenticationManager,
			PasswordEncoder passwordEncoder,
			RolesRepository rolesRepository
	) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.rolesRepository = rolesRepository;
	}

	@Transactional
	public User signup(RegisterUserDto registerUserDto) {
		var role = rolesRepository.findByNameWithPrivileges(registerUserDto.getRole())
				.orElseThrow(() -> new RuntimeException("Role not found"));
		var user = new User(registerUserDto, role, passwordEncoder.encode(registerUserDto.getPassword()));
		return userRepository.save(user);
	}

	public AuthResult authenticate(LoginUserDto input) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(input.getUsername(), input.getPassword())
		);

		Object principal = authentication.getPrincipal();

		User user;
		if (principal instanceof User u) {
			user = u;
		} else if (principal instanceof UserDetails ud) {
			user = userRepository.findByUsernameWithRolesAndPrivileges(ud.getUsername())
					.orElseThrow(() -> new RuntimeException("User not found"));
		} else {
			throw new RuntimeException("Invalid principal type: " + principal.getClass().getName());
		}

		// Fetch company IDs here instead of in controller
		List<Long> companyIds = userRepository.findCompanyIdsByUsername(user.getUsername());

		return new AuthResult(user, companyIds);
	}


	@Transactional
	public UserDto changePassword(ChangePasswordDto changePasswordDto) {
		var user = userRepository.findByUsernameWithRolesAndPrivileges(changePasswordDto.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		if (!passwordEncoder.matches(changePasswordDto.getOldPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Old password is incorrect");
		}
		user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
		var savedUser = userRepository.save(user);
		return UserMapper.toUserDto(savedUser);
	}
}
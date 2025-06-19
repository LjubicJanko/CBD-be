package cbd.order_tracker.service;

import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.ChangePasswordDto;
import cbd.order_tracker.model.dto.LoginUserDto;
import cbd.order_tracker.model.dto.RegisterUserDto;
import cbd.order_tracker.model.dto.UserDto;
import cbd.order_tracker.repository.RolesRepository;
import cbd.order_tracker.repository.UserRepository;
import cbd.order_tracker.util.UserMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	@Transactional(readOnly = true)
	public User authenticate(LoginUserDto input) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						input.getUsername(),
						input.getPassword()
				)
		);
		return userRepository.findByUsernameWithRolesAndPrivileges(input.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
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
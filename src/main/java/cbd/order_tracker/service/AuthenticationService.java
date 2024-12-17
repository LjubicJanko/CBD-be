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

import java.util.ArrayList;
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

	public User signup(RegisterUserDto registerUserDto) {
		var role = rolesRepository.findByName(registerUserDto.getRole());
		var user = new User(registerUserDto, role, passwordEncoder.encode(registerUserDto.getPassword()));

		return userRepository.save(user);
	}

	public User authenticate(LoginUserDto input) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						input.getUsername(),
						input.getPassword()
				)
		);

		return userRepository.findByUsername(input.getUsername()).orElseThrow();
	}


	public UserDto changePassword(ChangePasswordDto changePasswordDto) {
		var user = userRepository.findByUsername(changePasswordDto.getUsername()).orElseThrow();

		// Verify old password
		if (!passwordEncoder.matches(changePasswordDto.getOldPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Old password is incorrect");
		}

		// Update password
		user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
		var savedUser = userRepository.save(user);

		return UserMapper.toUserDto(savedUser);
	}

}

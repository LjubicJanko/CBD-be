package cbd.order_tracker.service;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.exceptions.RoleNotFoundException;
import cbd.order_tracker.exceptions.TenantNotFoundException;
import cbd.order_tracker.exceptions.UserNotFoundException;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.ChangePasswordDto;
import cbd.order_tracker.model.dto.LoginUserDto;
import cbd.order_tracker.model.dto.RegisterUserDto;
import cbd.order_tracker.model.dto.UserDto;
import cbd.order_tracker.repository.RolesRepository;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.repository.UserRepository;
import cbd.order_tracker.util.UserMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final RolesRepository rolesRepository;
	private final TenantRepository tenantRepository;

	public AuthenticationService(
			UserRepository userRepository,
			AuthenticationManager authenticationManager,
			PasswordEncoder passwordEncoder,
			RolesRepository rolesRepository,
			TenantRepository tenantRepository
	) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.rolesRepository = rolesRepository;
		this.tenantRepository = tenantRepository;
	}

	@Transactional
	public User signup(RegisterUserDto registerUserDto) {
		var role = rolesRepository.findByNameWithPrivileges(registerUserDto.getRole())
				.orElseThrow(() -> new RoleNotFoundException("Role not found"));

		Tenant tenant;
		if (TenantContext.isSuperadmin() && registerUserDto.getTenantId() != null) {
			tenant = tenantRepository.findById(registerUserDto.getTenantId())
					.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		} else {
			Long tenantId = TenantContext.getTenantId();
			if (tenantId == null) {
				throw new IllegalStateException("No tenant context for user creation");
			}
			tenant = tenantRepository.findById(tenantId)
					.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		}

		var user = new User(registerUserDto, role, passwordEncoder.encode(registerUserDto.getPassword()), tenant);
		return userRepository.save(user);
	}

	public User authenticate(LoginUserDto input) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						input.getUsername(),
						input.getPassword()
				)
		);

		Object principal = authentication.getPrincipal();

		User user;
		if (principal instanceof User u) {
			user = u;
		} else if (principal instanceof UserDetails userDetails) {
			user = userRepository.findByUsernameWithRolesAndPrivileges(userDetails.getUsername())
					.orElseThrow(() -> new UserNotFoundException("User not found"));
		} else {
			throw new RuntimeException("Invalid principal type: " + principal.getClass().getName());
		}

		Tenant tenant = user.getTenant();
		if (!user.isSuperadmin() && (tenant == null || !tenant.isActive())) {
			throw new BadCredentialsException("Tenant is inactive");
		}
		return user;
	}

	@Transactional
	public UserDto changePassword(ChangePasswordDto changePasswordDto) {
		var user = userRepository.findByUsernameWithRolesAndPrivileges(changePasswordDto.getUsername())
				.orElseThrow(() -> new UserNotFoundException("User not found"));
		if (!passwordEncoder.matches(changePasswordDto.getOldPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Old password is incorrect");
		}
		user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
		var savedUser = userRepository.save(user);
		return UserMapper.toUserDto(savedUser);
	}
}
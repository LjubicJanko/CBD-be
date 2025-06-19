package cbd.order_tracker.util;

import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.User;
import cbd.order_tracker.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;


public class UserUtil {
	private final UserRepository userRepository;

	public UserUtil(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public Collection<Role> getCurrentUserRoles() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			throw new RuntimeException("No authenticated user found");
		}
		String username = auth.getName();
		User user = userRepository.findByUsernameWithRoles(username)
				.orElseThrow(() -> new RuntimeException("User not found"));
		return user.getRoles();
	}

	public static String getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null && authentication.isAuthenticated()) {
			Object principal = authentication.getPrincipal();
			System.out.println(principal);
			if (principal instanceof User) {
				return ((User) principal).getFullName();
			} else {
				return principal.toString();
			}
		}

		return null;
	}

}

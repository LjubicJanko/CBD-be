package cbd.order_tracker.util;

import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.User;
import cbd.order_tracker.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;


@Component
public class UserUtil {

	public Set<Role> getCurrentUserRoles() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			throw new RuntimeException("No authenticated user found");
		}

		Object principal = auth.getPrincipal();

		if (principal instanceof User user) {
			return user.getRoles();
		} else {
			throw new RuntimeException("Unexpected principal type");
		}
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

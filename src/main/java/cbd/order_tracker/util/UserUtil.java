package cbd.order_tracker.util;

import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;


public class UserUtil {

	private UserUtil() {
	}

	public static Collection<Role> getCurrentUserRoles() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null && authentication.isAuthenticated()) {
			Object principal = authentication.getPrincipal();
			System.out.println(principal);
			if (principal instanceof User) {
				return ((User) principal).getRoles();
			}
		}

		return null;
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

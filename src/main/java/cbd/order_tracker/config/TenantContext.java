package cbd.order_tracker.config;

import org.springframework.security.access.AccessDeniedException;

import java.util.Set;

public class TenantContext {

	private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> superadmin = ThreadLocal.withInitial(() -> false);
	private static final ThreadLocal<Set<String>> features = new ThreadLocal<>();

	public static Long getTenantId() {
		return currentTenantId.get();
	}

	public static Long requireTenantId() {
		Long id = currentTenantId.get();
		if (id == null) {
			throw new AccessDeniedException("No tenant context — superadmin must send X-Tenant-Id");
		}
		return id;
	}

	public static void setTenantId(Long tenantId) {
		currentTenantId.set(tenantId);
	}

	public static boolean isSuperadmin() {
		return Boolean.TRUE.equals(superadmin.get());
	}

	public static void setSuperadmin(boolean value) {
		superadmin.set(value);
	}

	public static Set<String> getFeatures() {
		return features.get();
	}

	public static void setFeatures(Set<String> tenantFeatures) {
		features.set(tenantFeatures);
	}

	public static void clear() {
		currentTenantId.remove();
		superadmin.remove();
		features.remove();
	}
}

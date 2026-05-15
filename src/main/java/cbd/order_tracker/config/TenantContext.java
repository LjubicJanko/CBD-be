package cbd.order_tracker.config;

import org.springframework.security.access.AccessDeniedException;

public class TenantContext {

	private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> superadmin = ThreadLocal.withInitial(() -> false);

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

	public static void clear() {
		currentTenantId.remove();
		superadmin.remove();
	}
}

package cbd.order_tracker.config;

import cbd.order_tracker.model.Tenant;
import org.springframework.security.access.AccessDeniedException;

public class TenantGuard {

	public static void assertTenantMatch(Tenant entityTenant) {
		if (TenantContext.isSuperadmin()) {
			return;
		}
		Long currentTenantId = TenantContext.getTenantId();
		if (currentTenantId == null) {
			throw new AccessDeniedException("No tenant context");
		}
		if (entityTenant == null || !entityTenant.getId().equals(currentTenantId)) {
			throw new AccessDeniedException("Cross-tenant access denied");
		}
	}
}

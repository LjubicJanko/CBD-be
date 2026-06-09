package cbd.order_tracker.config;

import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * Enforces per-tenant feature flags. A tenant lacking a feature gets 403,
 * independent of the user's privileges (effective access = tenant-has-feature
 * AND user-has-privilege).
 *
 * <p>Superadmins bypass feature gates, mirroring {@link TenantGuard}.
 */
@Component
@RequiredArgsConstructor
public class FeatureGuard {

	private final TenantRepository tenantRepository;

	/**
	 * Authenticated/impersonation path: checks the feature against the tenant
	 * resolved into {@link TenantContext} by the JWT filter.
	 */
	public void requireFeature(String featureKey) {
		if (TenantContext.isSuperadmin()) {
			return;
		}
		Set<String> features = TenantContext.getFeatures();
		if (features == null || !features.contains(featureKey)) {
			throw new AccessDeniedException("Feature '" + featureKey + "' is not enabled for this tenant");
		}
	}

	/** Public path with an already-resolved tenant (e.g. order-extension by slug). */
	public void requireFeature(Tenant tenant, String featureKey) {
		Set<String> features = tenant != null ? tenant.getFeatures() : null;
		if (features == null || !features.contains(featureKey)) {
			throw new AccessDeniedException("Feature '" + featureKey + "' is not enabled for this tenant");
		}
	}

	/** Public path that resolves the tenant by slug, then checks the feature. */
	public void requireFeatureBySlug(String slug, String featureKey) {
		Tenant tenant = tenantRepository.findBySlug(slug.toLowerCase())
				.filter(Tenant::isActive)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
		requireFeature(tenant, featureKey);
	}
}

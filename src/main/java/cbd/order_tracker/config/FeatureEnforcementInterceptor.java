package cbd.order_tracker.config;

import cbd.order_tracker.model.enums.Feature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Central feature gate for authenticated, tenant-scoped endpoints. Maps URL
 * prefixes to the feature they require and rejects (403) when the caller's
 * tenant lacks it — independent of the user's privileges.
 *
 * <p>Public endpoints are NOT covered here (the tenant is resolved by slug
 * inside their controllers); they call {@link FeatureGuard} directly:
 * <ul>
 *   <li>{@code /api/orderExtend/**} and {@code /api/orders/track/**} -> order-extension</li>
 * </ul>
 * Those two prefixes are therefore excluded below so this interceptor never
 * 403s a tenant-less public request.
 */
@RequiredArgsConstructor
public class FeatureEnforcementInterceptor implements HandlerInterceptor {

	private final FeatureGuard featureGuard;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		// Let CORS preflight through untouched.
		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			return true;
		}
		String feature = requiredFeature(request.getRequestURI());
		if (feature != null) {
			featureGuard.requireFeature(feature); // throws AccessDeniedException -> 403
		}
		return true;
	}

	private String requiredFeature(String path) {
		// Public sub-paths handled in-controller; must not be gated here.
		if (path.startsWith("/api/orders/track/")) {
			return null;
		}
		if (path.startsWith("/api/orders/")) {
			return Feature.ORDERS.getKey();
		}
		if (path.startsWith("/api/banners/active/")) {
			return null;
		}
		if (path.startsWith("/api/banners/")) {
			return Feature.BANNERS.getKey();
		}
		if (path.startsWith("/api/attendance") || path.startsWith("/api/locations")) {
			return Feature.ATTENDANCE.getKey();
		}
		if (path.startsWith("/api/reports")) {
			return Feature.REPORTS.getKey();
		}
		return null;
	}
}

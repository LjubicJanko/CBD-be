package cbd.order_tracker.model.enums;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Per-tenant premium feature/module keys. The kebab-case key is the contract
 * with the frontend (matched literally) and is what gets persisted in
 * {@code tenant_features}. Do not rename keys.
 */
public enum Feature {
	ORDERS("orders"),
	ORDER_EXTENSION("order-extension"),
	BANNERS("banners"),
	ATTENDANCE("attendance"),
	// Sub-feature of ATTENDANCE — gates the *ability to configure* GPS/geofence check-in
	// locations (create one, or switch an existing location to GEOFENCE). QR is the
	// default, always-on check-in method under the base ATTENDANCE feature; geofence is
	// the opt-in beta path, being the more failure-prone of the two (permission prompts,
	// spoofable via mock-location apps, device accuracy variance). Does NOT gate an
	// already-configured geofence location: it must keep working even if this is later
	// disabled — see WorkLocationServiceImpl.requireGeofenceFeatureEnabled for why the
	// check fires on *requesting* the switch, not on the location's resolved state.
	// Enforcement lives in WorkLocationServiceImpl, not the URL-prefix interceptor, since
	// it gates a field value, not a whole endpoint. Not backend-enforced as depending on
	// ATTENDANCE (no feature-dependency validation exists in this codebase today — see
	// PlatformServiceImpl.applyFeatures) so treat that pairing as FE-form-only, same as
	// ORDER_EXTENSION/ORDERS.
	ATTENDANCE_GEOFENCE("attendance-geofence"),
	REPORTS("reports"),
	THEMING("theming");

	private final String key;

	Feature(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	/** All known feature keys. */
	public static final Set<String> KEYS = Arrays.stream(values())
			.map(Feature::getKey)
			.collect(Collectors.toUnmodifiableSet());

	/**
	 * Subset safe to expose to anonymous/public callers — only modules that
	 * drive public-facing pages. Back-office modules (orders, attendance,
	 * reports) are intentionally withheld from the public projection.
	 */
	public static final Set<String> PUBLIC_KEYS = Set.of(ORDER_EXTENSION.key, BANNERS.key, THEMING.key);

	/** Backend-owned defaults seeded on tenant creation. */
	public static Set<String> defaultKeys() {
		return new LinkedHashSet<>(Set.of(ORDERS.key, ORDER_EXTENSION.key, BANNERS.key));
	}

	public static boolean isValidKey(String key) {
		return KEYS.contains(key);
	}
}

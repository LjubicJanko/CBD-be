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

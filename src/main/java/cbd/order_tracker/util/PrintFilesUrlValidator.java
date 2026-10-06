package cbd.order_tracker.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Validates the optional "print files" link attached to an order at PRINT_READY.
 * Accepts an absolute https URL of at most 2048 characters, without userinfo,
 * whitespace or control characters. Host policy: {@link #ALLOWED_HOSTS} empty means any host;
 * non-empty means exact, case-insensitive host match.
 */
public final class PrintFilesUrlValidator {

	public static final int MAX_LENGTH = 2048;

	/** Code-constant host policy. Empty = any https host. */
	public static final Set<String> ALLOWED_HOSTS = Set.of();

	private PrintFilesUrlValidator() {
	}

	/** Trims the value; returns null when it is null or blank (meaning "not provided"/clear). */
	public static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/** Validates an already normalized (trimmed, non-blank) value against {@link #ALLOWED_HOSTS}. */
	public static void validate(String value) {
		validate(value, ALLOWED_HOSTS);
	}

	/** @throws IllegalArgumentException when the value is not acceptable */
	public static void validate(String value, Set<String> allowedHosts) {
		if (value.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("printFilesUrl must be at most " + MAX_LENGTH + " characters");
		}
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (Character.isWhitespace(c) || Character.isISOControl(c)) {
				throw new IllegalArgumentException("printFilesUrl must not contain whitespace or control characters");
			}
		}
		URI uri;
		try {
			uri = new URI(value);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("printFilesUrl must be a valid https URL");
		}
		if (!"https".equalsIgnoreCase(uri.getScheme())) {
			throw new IllegalArgumentException("printFilesUrl must be an absolute https URL");
		}
		if (uri.getRawUserInfo() != null) {
			throw new IllegalArgumentException("printFilesUrl must not contain user credentials");
		}
		String host = uri.getHost();
		if (host == null || host.isEmpty()) {
			throw new IllegalArgumentException("printFilesUrl must contain a host");
		}
		if (!allowedHosts.isEmpty()) {
			String lowerHost = host.toLowerCase(Locale.ROOT);
			boolean allowed = allowedHosts.stream().anyMatch(h -> h.toLowerCase(Locale.ROOT).equals(lowerHost));
			if (!allowed) {
				throw new IllegalArgumentException("printFilesUrl host is not allowed");
			}
		}
	}
}

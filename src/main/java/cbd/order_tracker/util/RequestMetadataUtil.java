package cbd.order_tracker.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestMetadataUtil {

	private RequestMetadataUtil() {}

	// Prefer the first entry of X-Forwarded-For when present (left-most = original client),
	// since the app sits behind a trusted proxy / Cloudflare-style ingress. Fall back to
	// the direct socket address.
	public static String clientIp(HttpServletRequest req) {
		if (req == null) return null;
		String xff = req.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			int comma = xff.indexOf(',');
			String first = (comma < 0 ? xff : xff.substring(0, comma)).trim();
			if (!first.isEmpty()) return truncate(first, 45);
		}
		String realIp = req.getHeader("X-Real-IP");
		if (realIp != null && !realIp.isBlank()) {
			return truncate(realIp.trim(), 45);
		}
		return truncate(req.getRemoteAddr(), 45);
	}

	public static String userAgent(HttpServletRequest req) {
		if (req == null) return null;
		String ua = req.getHeader("User-Agent");
		return truncate(ua, 500);
	}

	private static String truncate(String s, int max) {
		if (s == null) return null;
		return s.length() <= max ? s : s.substring(0, max);
	}
}

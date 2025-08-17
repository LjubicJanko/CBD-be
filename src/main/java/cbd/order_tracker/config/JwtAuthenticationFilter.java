package cbd.order_tracker.config;

import cbd.order_tracker.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final HandlerExceptionResolver handlerExceptionResolver;
	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	public JwtAuthenticationFilter(
			JwtService jwtService,
			UserDetailsService userDetailsService,
			HandlerExceptionResolver handlerExceptionResolver
	) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.handlerExceptionResolver = handlerExceptionResolver;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {

		final String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			final String jwt = authHeader.substring(7);
			final String username = jwtService.extractUsername(jwt);

			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
				if (jwtService.isTokenValid(jwt, userDetails)) {
					UsernamePasswordAuthenticationToken authToken =
							new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

					// Read claims we put in the token
					var claims = jwtService.getAllClaims(jwt);
					@SuppressWarnings("unchecked")
					var companyIds = new java.util.HashSet<Long>(
							((java.util.List<?>) claims.getOrDefault("companyIds", java.util.List.of()))
									.stream().map(x -> ((Number) x).longValue()).toList()
					);
					@SuppressWarnings("unchecked")
					var roles = new java.util.HashSet<String>((java.util.List<String>) claims.getOrDefault("roles", java.util.List.of()));

					// You can get userId from DB or store in token as well; for now null-safe
					Integer userId = null; // or include in claims similarly
					String uname = userDetails.getUsername();

					var ctx = new RequestUserContext(userId, uname, companyIds, roles);
					authToken.setDetails(ctx);

					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					// IMPORTANT: keep our ctx, so set it AFTER WebAuthenticationDetailsSource if you use that:
					authToken.setDetails(ctx);

					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
			filterChain.doFilter(request, response);

		} catch (io.jsonwebtoken.ExpiredJwtException ex) {
			response.setStatus(498);
			response.setContentType("application/json");
			response.getWriter().write("{\"status\": 498, \"message\": \"JWT token expired\", \"description\": \"The JWT token has expired.\"}");
		} catch (Exception exception) {
			handlerExceptionResolver.resolveException(request, response, null, exception);
		}
	}
}
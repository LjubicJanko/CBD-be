package cbd.order_tracker.config;

import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final HandlerExceptionResolver handlerExceptionResolver;
	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final TenantRepository tenantRepository;

	public JwtAuthenticationFilter(
			JwtService jwtService,
			UserDetailsService userDetailsService,
			HandlerExceptionResolver handlerExceptionResolver,
			TenantRepository tenantRepository
	) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.handlerExceptionResolver = handlerExceptionResolver;
		this.tenantRepository = tenantRepository;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {
		TenantContext.clear();

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
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
							userDetails,
							null,
							userDetails.getAuthorities()
					);
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);

					boolean isSuperadmin = jwtService.extractSuperadmin(jwt);
					TenantContext.setSuperadmin(isSuperadmin);

					if (isSuperadmin) {
						String headerTenantId = request.getHeader("X-Tenant-Id");
						if (headerTenantId != null && !headerTenantId.isBlank()) {
							Long parsed;
							try {
								parsed = Long.parseLong(headerTenantId);
							} catch (NumberFormatException ex) {
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								response.setContentType("application/json");
								response.getWriter().write("{\"status\": 400, \"message\": \"Invalid X-Tenant-Id header\"}");
								return;
							}
							Optional<Tenant> tenant = tenantRepository.findById(parsed);
							if (tenant.isEmpty()) {
								response.setStatus(HttpServletResponse.SC_FORBIDDEN);
								response.setContentType("application/json");
								response.getWriter().write("{\"status\": 403, \"message\": \"Selected tenant does not exist\"}");
								return;
							}
							if (!tenant.get().isActive()) {
								response.setStatus(HttpServletResponse.SC_FORBIDDEN);
								response.setContentType("application/json");
								response.getWriter().write("{\"status\": 403, \"message\": \"Selected tenant is deactivated\"}");
								return;
							}
							TenantContext.setTenantId(parsed);
							log.info("Superadmin impersonation: user={} targetTenantId={} method={} uri={}",
									username, parsed, request.getMethod(), request.getRequestURI());
						}
					} else {
						Long tenantId = jwtService.extractTenantId(jwt);
						if (tenantId == null) {
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.setContentType("application/json");
							response.getWriter().write("{\"status\": 403, \"message\": \"Token has no tenant claim\"}");
							return;
						}
						Optional<Tenant> tenant = tenantRepository.findById(tenantId);
						if (tenant.isEmpty() || !tenant.get().isActive()) {
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.setContentType("application/json");
							response.getWriter().write("{\"status\": 403, \"message\": \"Tenant is no longer active\"}");
							return;
						}
						TenantContext.setTenantId(tenantId);
					}
				}
			}
			filterChain.doFilter(request, response);
		} catch (ExpiredJwtException ex) {
			response.setStatus(498);
			response.setContentType("application/json");
			response.getWriter().write("{\"status\": 498, \"message\": \"JWT token expired\", \"description\": \"The JWT token has expired.\"}");
		} catch (Exception exception) {
			handlerExceptionResolver.resolveException(request, response, null, exception);
		} finally {
			TenantContext.clear();
		}
	}
}
package cbd.order_tracker.model;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class LoginResponse {
	private Integer id;
	private String token;
	private long expiresIn;
	private Set<String> roles;
	private Set<String> privileges;
	private String name;
	private String username;
	private Long tenantId;
	private String tenantSlug;
	private String tenantLogoUrl;
	private boolean superadmin;
	private Set<String> features;

	public String getToken() {
		return token;
	}

	public LoginResponse setToken(String token) {
		this.token = token;
		return this;
	}

	public LoginResponse() {
	}

	public LoginResponse(Integer id, String token, long expiresIn, Collection<Role> roles, String fullName, String username,
					   Long tenantId, String tenantSlug, boolean superadmin) {
		this.id = id;
		this.token = token;
		this.expiresIn = expiresIn;
		this.roles = roles.stream()
				.map(Role::getName)
				.collect(Collectors.toSet());
		this.privileges = roles.stream()
				.flatMap(role -> role.getPrivileges().stream())
				.map(Privilege::getName)
				.collect(Collectors.toSet());
		this.name = fullName;
		this.username = username;
		this.tenantId = tenantId;
		this.tenantSlug = tenantSlug;
		this.superadmin = superadmin;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public long getExpiresIn() {
		return expiresIn;
	}

	public LoginResponse setExpiresIn(long expiresIn) {
		this.expiresIn = expiresIn;
		return this;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<String> getPrivileges() {
		return privileges;
	}

	public void setPrivileges(Set<String> privileges) {
		this.privileges = privileges;
	}

	public Long getTenantId() { return tenantId; }
	public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
	public String getTenantSlug() { return tenantSlug; }
	public void setTenantSlug(String tenantSlug) { this.tenantSlug = tenantSlug; }
	public String getTenantLogoUrl() { return tenantLogoUrl; }
	public void setTenantLogoUrl(String tenantLogoUrl) { this.tenantLogoUrl = tenantLogoUrl; }
	public boolean isSuperadmin() { return superadmin; }
	public void setSuperadmin(boolean superadmin) { this.superadmin = superadmin; }
	public Set<String> getFeatures() { return features; }
	public void setFeatures(Set<String> features) { this.features = features; }

	@Override
	public String toString() {
		return "LoginResponse{username='" + username + "', expiresIn=" + expiresIn + ", superadmin=" + superadmin + '}';
	}
}

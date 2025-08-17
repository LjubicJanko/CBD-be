package cbd.order_tracker.model;

import cbd.order_tracker.model.company.Company;
import lombok.Data;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class LoginResponse {
	private Integer id;
	private String token;
	private long expiresIn;
	private Set<String> roles;
	private Set<String> privileges;
	private String name;
	private String username;
	private Set<Long> companyIds;

	public LoginResponse() {
	}


	public LoginResponse(Integer id, String token, long expiresIn,
						 Collection<Role> roles, String fullName,
						 String username, Set<Company> companies) {
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

		// If NOT super admin, include company IDs
		if (!this.roles.contains("super_admin")) {
			this.companyIds = companies.stream()
					.map(Company::getId)
					.collect(Collectors.toSet());
		}
	}

	public LoginResponse setExpiresIn(long expiresIn) {
		this.expiresIn = expiresIn;
		return this;
	}
	public LoginResponse setToken(String token) {
		this.token = token;
		return this;
	}

	@Override
	public String toString() {
		return "LoginResponse{" + "token='" + token + '\'' + ", expiresIn=" + expiresIn + '}';
	}
}

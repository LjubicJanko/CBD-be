package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.Role;

import java.time.LocalDateTime;
import java.util.Set;

public class UserDto {
	private Integer id;
	private String fullName;
	private String username;
	private LocalDateTime createdAt;
	private Set<Role> roles;

	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }
	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public Set<Role> getRoles() { return roles; }
	public void setRoles(Set<Role> roles) { this.roles = roles; }
}
package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.Role;

import java.util.Date;
import java.util.Set;

public class UserDto {
	private String fullName;
	private String username;
	private Date createdAt;
	private Set<Role> roles;

	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }
	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
	public Set<Role> getRoles() { return roles; }
	public void setRoles(Set<Role> roles) { this.roles = roles; }
}
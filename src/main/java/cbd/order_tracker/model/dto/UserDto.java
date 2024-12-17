package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.User;

import java.util.Collection;
import java.util.Date;

public class UserDto {
	private String fullName;
	private String username;
	private Date createdAt;
	private Collection<Role> roles;

	public UserDto() {
	}

	public UserDto(User user) {
		this.fullName = user.getFullName();
		this.username = user.getUsername();
		this.createdAt = user.getCreatedAt();
		this.roles = user.getRoles();
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Collection<Role> getRoles() {
		return roles;
	}

	public void setRoles(Collection<Role> roles) {
		this.roles = roles;
	}
}

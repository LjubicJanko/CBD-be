package cbd.order_tracker.util;

import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.UserDto;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class UserMapper {
	public static UserDto toUserDto(User user) {
		UserDto userDto = new UserDto();
		userDto.setUsername(user.getUsername());
		userDto.setFullName(user.getFullName());
		if (user.getCreatedAt() != null) {
			userDto.setCreatedAt(user.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
		}
		userDto.setRoles(user.getRoles());
		return userDto;
	}
}
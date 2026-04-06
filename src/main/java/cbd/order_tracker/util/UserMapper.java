package cbd.order_tracker.util;

import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.UserDto;

public class UserMapper {
	public static UserDto toUserDto(User user) {
		UserDto userDto = new UserDto();
		userDto.setUsername(user.getUsername());
		userDto.setFullName(user.getFullName());
		userDto.setCreatedAt(user.getCreatedAt());
		userDto.setRoles(user.getRoles());
		return userDto;
	}
}
package cbd.order_tracker.util;

import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.UserDto;

public class UserMapper {
	public static UserDto toUserDto(User user) {
		return new UserDto(user);
	}

}

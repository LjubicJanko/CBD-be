package cbd.order_tracker.service;

import cbd.order_tracker.model.dto.UserDto;
import cbd.order_tracker.repository.UserRepository;
import cbd.order_tracker.util.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<UserDto> allUsers() {
		return userRepository.findAllWithRolesAndPrivileges()
			.stream()
			.map(UserMapper::toUserDto)
			.collect(Collectors.toList());
	}
}

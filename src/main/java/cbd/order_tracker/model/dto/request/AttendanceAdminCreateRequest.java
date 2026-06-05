package cbd.order_tracker.model.dto.request;

import cbd.order_tracker.validation.PastOrPresentUtc;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendanceAdminCreateRequest {

	@NotNull
	private Integer userId;

	@NotNull
	private Long locationId;

	@NotNull
	@PastOrPresentUtc
	private LocalDateTime checkInAt;

	@PastOrPresentUtc
	private LocalDateTime checkOutAt;

	private String notes;
}

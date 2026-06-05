package cbd.order_tracker.model.dto.request;

import cbd.order_tracker.validation.PastOrPresentUtc;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendanceAdminPatchRequest {

	@PastOrPresentUtc
	private LocalDateTime checkInAt;

	@PastOrPresentUtc
	private LocalDateTime checkOutAt;

	private String notes;
}

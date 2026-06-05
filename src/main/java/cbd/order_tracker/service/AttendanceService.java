package cbd.order_tracker.service;

import cbd.order_tracker.model.dto.PageableResponse;
import cbd.order_tracker.model.dto.request.AttendanceAdminCreateRequest;
import cbd.order_tracker.model.dto.request.AttendanceAdminPatchRequest;
import cbd.order_tracker.model.dto.request.AttendanceCheckRequest;
import cbd.order_tracker.model.dto.response.AttendanceSessionDto;
import cbd.order_tracker.model.dto.response.CheckOutResponseDto;
import cbd.order_tracker.model.dto.response.CurrentSessionDto;

import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceService {

	CurrentSessionDto checkIn(AttendanceCheckRequest req, String ip, String userAgent);

	CheckOutResponseDto checkOut(AttendanceCheckRequest req, String ip, String userAgent);

	Optional<CurrentSessionDto> currentForUser();

	PageableResponse<AttendanceSessionDto> search(
			LocalDate date,
			LocalDate from,
			LocalDate to,
			Integer userId,
			Long locationId,
			boolean openOnly,
			int page,
			int size,
			String sort
	);

	AttendanceSessionDto adminCreate(AttendanceAdminCreateRequest req);

	AttendanceSessionDto adminPatch(Long id, AttendanceAdminPatchRequest req);

	int autoCloseStaleSessions();
}

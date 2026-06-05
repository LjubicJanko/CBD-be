package cbd.order_tracker.util;

import cbd.order_tracker.model.AttendanceSession;
import cbd.order_tracker.model.WorkLocation;
import cbd.order_tracker.model.dto.response.AttendanceSessionDto;
import cbd.order_tracker.model.dto.response.CheckOutResponseDto;
import cbd.order_tracker.model.dto.response.CurrentSessionDto;
import cbd.order_tracker.model.dto.response.WorkLocationDto;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class AttendanceMapper {

	private AttendanceMapper() {}

	public static Instant toInstant(LocalDateTime ldt) {
		return ldt == null ? null : ldt.toInstant(ZoneOffset.UTC);
	}

	public static WorkLocationDto toDto(WorkLocation l) {
		return new WorkLocationDto(
				l.getId(),
				l.getName(),
				l.getLat(),
				l.getLng(),
				l.getRadiusM(),
				l.isActive(),
				toInstant(l.getCreatedAt()),
				toInstant(l.getUpdatedAt())
		);
	}

	public static CurrentSessionDto toCurrentSessionDto(AttendanceSession s) {
		return new CurrentSessionDto(
				s.getId(),
				s.getLocation().getId(),
				s.getLocation().getName(),
				toInstant(s.getCheckInAt())
		);
	}

	public static CheckOutResponseDto toCheckOutResponseDto(AttendanceSession s) {
		Long duration = (s.getCheckInAt() != null && s.getCheckOutAt() != null)
				? Duration.between(s.getCheckInAt(), s.getCheckOutAt()).getSeconds()
				: null;
		return new CheckOutResponseDto(
				s.getId(),
				s.getLocation().getId(),
				s.getLocation().getName(),
				toInstant(s.getCheckInAt()),
				toInstant(s.getCheckOutAt()),
				duration
		);
	}

	public static AttendanceSessionDto toSessionDto(AttendanceSession s) {
		Long duration = (s.getCheckInAt() != null && s.getCheckOutAt() != null)
				? Duration.between(s.getCheckInAt(), s.getCheckOutAt()).getSeconds()
				: null;
		return new AttendanceSessionDto(
				s.getId(),
				s.getUser().getId(),
				s.getUser().getUsername(),
				s.getUser().getFullName(),
				s.getLocation().getId(),
				s.getLocation().getName(),
				toInstant(s.getCheckInAt()),
				s.getCheckInLat(),
				s.getCheckInLng(),
				s.getCheckInAccuracyM(),
				s.getCheckInIp(),
				s.getCheckInUserAgent(),
				toInstant(s.getCheckOutAt()),
				s.getCheckOutLat(),
				s.getCheckOutLng(),
				s.getCheckOutAccuracyM(),
				s.getCheckOutIp(),
				s.getCheckOutUserAgent(),
				s.isAutoClosed(),
				s.getNotes(),
				duration
		);
	}
}

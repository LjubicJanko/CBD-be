package cbd.order_tracker.service.impl;

import cbd.order_tracker.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceAutoCloseJob {

	private static final Logger log = LoggerFactory.getLogger(AttendanceAutoCloseJob.class);

	private final AttendanceService attendanceService;

	// Daily at 03:00 UTC (v1).
	@Scheduled(cron = "0 0 3 * * *", zone = "UTC")
	public void run() {
		try {
			int closed = attendanceService.autoCloseStaleSessions();
			log.info("Attendance auto-close job complete: {} sessions closed", closed);
		} catch (Exception e) {
			log.error("Attendance auto-close job failed", e);
		}
	}
}

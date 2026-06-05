package cbd.order_tracker.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSessionDto {
	private Long id;
	private Integer userId;
	private String username;
	private String fullName;
	private Long locationId;
	private String locationName;
	private Instant checkInAt;
	private BigDecimal checkInLat;
	private BigDecimal checkInLng;
	private Integer checkInAccuracyM;
	private String checkInIp;
	private String checkInUserAgent;
	private Instant checkOutAt;
	private BigDecimal checkOutLat;
	private BigDecimal checkOutLng;
	private Integer checkOutAccuracyM;
	private String checkOutIp;
	private String checkOutUserAgent;
	private boolean autoClosed;
	private String notes;
	private Long durationSeconds;
}

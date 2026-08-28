package cbd.order_tracker.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Result of a QR scan toggle — action tells the FE which of the two happened. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanResultDto {
	private String action; // "CHECK_IN" | "CHECK_OUT"
	private Long sessionId;
	private Long locationId;
	private String locationName;
	private Instant checkInAt;
	private Instant checkOutAt;
	private Long durationSeconds;
}

package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.CheckInMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkLocationDto {
	private Long id;
	private String name;
	private BigDecimal lat;
	private BigDecimal lng;
	private Integer radiusM;
	private boolean active;
	private CheckInMethod checkInMethod;
	// qrToken and openSessionCount are populated only for callers with location-manage —
	// anyone who can check in can list locations (for name rendering), but the QR token is
	// the security boundary for QR check-ins, so it must not leak to non-admins.
	private String qrToken;
	private Long openSessionCount;
	private Instant createdAt;
	private Instant updatedAt;
}

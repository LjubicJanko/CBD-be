package cbd.order_tracker.model.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * GPS is optional here (unlike {@link AttendanceCheckRequest}) — QR check-ins are gated by
 * possession of the printed code, not by location, so coordinates are recorded for audit
 * only when the client managed to get a fix.
 */
@Data
public class AttendanceScanRequest {

	@DecimalMin(value = "-90.0")
	@DecimalMax(value = "90.0")
	private BigDecimal lat;

	@DecimalMin(value = "-180.0")
	@DecimalMax(value = "180.0")
	private BigDecimal lng;

	@Min(0)
	private Integer accuracy;
}

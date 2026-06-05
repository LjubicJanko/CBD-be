package cbd.order_tracker.model.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AttendanceCheckRequest {

	@NotNull
	@DecimalMin(value = "-90.0")
	@DecimalMax(value = "90.0")
	private BigDecimal lat;

	@NotNull
	@DecimalMin(value = "-180.0")
	@DecimalMax(value = "180.0")
	private BigDecimal lng;

	@NotNull
	@Min(0)
	private Integer accuracy;
}

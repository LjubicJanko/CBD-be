package cbd.order_tracker.model.dto.request;

import cbd.order_tracker.model.CheckInMethod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkLocationRequest {

	@NotBlank
	@Size(max = 120)
	private String name;

	@NotNull
	private CheckInMethod checkInMethod;

	// Required when checkInMethod = GEOFENCE; ignored for QR (validated in the service,
	// since the requirement is conditional on checkInMethod).
	@DecimalMin(value = "-90.0")
	@DecimalMax(value = "90.0")
	private BigDecimal lat;

	@DecimalMin(value = "-180.0")
	@DecimalMax(value = "180.0")
	private BigDecimal lng;

	@Min(10)
	@Max(5000)
	private Integer radiusM;

	private Boolean active;
}

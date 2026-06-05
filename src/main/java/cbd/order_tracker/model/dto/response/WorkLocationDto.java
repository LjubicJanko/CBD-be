package cbd.order_tracker.model.dto.response;

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
	private Instant createdAt;
	private Instant updatedAt;
}

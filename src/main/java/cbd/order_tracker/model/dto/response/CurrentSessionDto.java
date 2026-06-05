package cbd.order_tracker.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentSessionDto {
	private Long id;
	private Long locationId;
	private String locationName;
	private Instant checkInAt;
}

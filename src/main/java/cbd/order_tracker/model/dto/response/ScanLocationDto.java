package cbd.order_tracker.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Public, pre-auth response for the QR landing page — deliberately minimal. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanLocationDto {
	private String locationName;
}

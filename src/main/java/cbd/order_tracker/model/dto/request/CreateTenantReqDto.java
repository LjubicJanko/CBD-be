package cbd.order_tracker.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTenantReqDto {

	@NotBlank
	@Size(min = 1, max = 120)
	private String name;

	@NotBlank
	@Size(min = 2, max = 64)
	@Pattern(regexp = "^[a-z0-9-]+$", message = "slug must contain only lowercase letters, digits, and hyphens")
	private String slug;
}

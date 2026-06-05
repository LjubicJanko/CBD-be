package cbd.order_tracker.model.dto.request;

import cbd.order_tracker.model.dto.SocialLinkDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body for a company_admin editing their OWN tenant. Deliberately exposes only
 * name + socialLink — slug and active are not editable here. Any stray slug/active
 * field in the JSON is ignored (Spring Boot's Jackson defaults to
 * fail-on-unknown-properties=false), so it never breaks the save.
 */
@Data
public class UpdateOwnTenantReqDto {

	@NotBlank
	@Size(min = 1, max = 120)
	private String name;

	// Same null-vs-omitted semantics as the platform endpoint: explicit null clears,
	// an omitted field leaves the link unchanged (tracked via socialLinkProvided).
	@Valid
	private SocialLinkDto socialLink;

	@JsonIgnore
	private boolean socialLinkProvided = false;

	public void setSocialLink(SocialLinkDto socialLink) {
		this.socialLink = socialLink;
		this.socialLinkProvided = true;
	}
}

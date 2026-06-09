package cbd.order_tracker.model.dto.request;

import cbd.order_tracker.model.dto.SocialLinkDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateTenantReqDto {

	@NotBlank
	@Size(min = 1, max = 120)
	private String name;

	@NotBlank
	@Size(min = 2, max = 64)
	@Pattern(regexp = "^[a-z0-9-]+$", message = "slug must contain only lowercase letters, digits, and hyphens")
	private String slug;

	// Optional. On update: an explicit `null` clears the link, while an OMITTED
	// field leaves it unchanged (tracked via socialLinkProvided). On create:
	// present -> set, omitted/null -> no link. @Valid cascades validation only
	// when the object is present.
	@Valid
	private SocialLinkDto socialLink;

	@JsonIgnore
	private boolean socialLinkProvided = false;

	// Optional, honored on UPDATE only (creation defaults are backend-owned).
	// Validated against Feature.KEYS in the service. Tracked null-vs-omitted:
	// an omitted field leaves features unchanged; a present array (even empty)
	// replaces the set.
	private List<String> features;

	@JsonIgnore
	private boolean featuresProvided = false;

	public void setSocialLink(SocialLinkDto socialLink) {
		this.socialLink = socialLink;
		this.socialLinkProvided = true;
	}

	public void setFeatures(List<String> features) {
		this.features = features;
		this.featuresProvided = true;
	}
}

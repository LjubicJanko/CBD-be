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

	// Optional theme colors. Same null-vs-omitted semantics as socialLink: an
	// omitted field leaves the stored color unchanged, an explicit null clears it
	// (-> FE default), a string sets it. 6-digit hex only; @Pattern passes for
	// null so "clear" is accepted. Stored uppercase (normalized in the service).
	@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "accentColor must be a 6-digit hex color, e.g. #D4FF00")
	private String accentColor;

	@JsonIgnore
	private boolean accentColorProvided = false;

	@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "backgroundColor must be a 6-digit hex color, e.g. #0B1120")
	private String backgroundColor;

	@JsonIgnore
	private boolean backgroundColorProvided = false;

	public void setSocialLink(SocialLinkDto socialLink) {
		this.socialLink = socialLink;
		this.socialLinkProvided = true;
	}

	public void setFeatures(List<String> features) {
		this.features = features;
		this.featuresProvided = true;
	}

	public void setAccentColor(String accentColor) {
		this.accentColor = accentColor;
		this.accentColorProvided = true;
	}

	public void setBackgroundColor(String backgroundColor) {
		this.backgroundColor = backgroundColor;
		this.backgroundColorProvided = true;
	}
}

package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.SocialLink;
import cbd.order_tracker.model.SocialLinkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/**
 * Nested social-link shape shared by tenant requests and responses. When this
 * object is present (non-null) every field is required — partial objects are
 * rejected, giving "all-or-nothing" semantics. Field-error keys are emitted
 * under the nested path: {@code socialLink.type}, {@code socialLink.url},
 * {@code socialLink.displayText}.
 */
@Data
@NoArgsConstructor
public class SocialLinkDto {

	@NotBlank(message = "type is required")
	@Pattern(regexp = "INSTAGRAM|FACEBOOK", message = "type must be INSTAGRAM or FACEBOOK")
	private String type;

	@NotBlank(message = "url is required")
	@Size(max = 2048, message = "url must be at most 2048 characters")
	@URL(regexp = "^https?://.+", message = "url must be a valid absolute http/https URL")
	private String url;

	@NotBlank(message = "displayText is required")
	@Size(max = 100, message = "displayText must be at most 100 characters")
	private String displayText;

	public SocialLinkDto(SocialLink socialLink) {
		this.type = socialLink.getType() != null ? socialLink.getType().name() : null;
		this.url = socialLink.getUrl();
		this.displayText = socialLink.getDisplayText();
	}

	/** Maps this validated request shape to a persistable embeddable. */
	public SocialLink toEntity() {
		return new SocialLink(SocialLinkType.valueOf(type), url, displayText);
	}
}

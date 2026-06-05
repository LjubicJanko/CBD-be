package cbd.order_tracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A tenant's single, optional social link. Stored as flat columns on the
 * tenant table; Hibernate maps an all-null component back to a null
 * {@code socialLink}, so an unset link is represented as {@code null}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class SocialLink {

	@Enumerated(EnumType.STRING)
	@Column(name = "social_link_type", length = 16)
	private SocialLinkType type;

	@Column(name = "social_link_url", length = 2048)
	private String url;

	@Column(name = "social_link_display_text", length = 100)
	private String displayText;
}

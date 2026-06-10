package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.dto.SocialLinkDto;
import cbd.order_tracker.model.enums.Feature;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class TenantPublicDto {
	private String name;
	private String slug;
	private String logoUrl;
	private SocialLinkDto socialLink;
	private Set<String> features;
	private String accentColor;
	private String backgroundColor;

	public TenantPublicDto(Tenant tenant) {
		this.name = tenant.getName();
		this.slug = tenant.getSlug();
		this.logoUrl = tenant.getLogoBytes() != null && tenant.getLogoBytes().length > 0
				? "/api/public/tenants/" + tenant.getSlug() + "/logo"
				: null;
		this.socialLink = tenant.getSocialLink() != null ? new SocialLinkDto(tenant.getSocialLink()) : null;
		// Only expose public-facing modules to anonymous callers.
		this.features = tenant.getFeatures() == null ? new LinkedHashSet<>()
				: tenant.getFeatures().stream()
						.filter(Feature.PUBLIC_KEYS::contains)
						.collect(Collectors.toCollection(LinkedHashSet::new));
		this.accentColor = tenant.getAccentColor();
		this.backgroundColor = tenant.getBackgroundColor();
	}
}

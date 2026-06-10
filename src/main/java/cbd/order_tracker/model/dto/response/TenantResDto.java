package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.dto.SocialLinkDto;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class TenantResDto {
	private Long id;
	private String name;
	private String slug;
	private boolean active;
	private LocalDateTime createdAt;
	private String logoUrl;
	private SocialLinkDto socialLink;
	private Set<String> features;
	private String accentColor;
	private String backgroundColor;

	public TenantResDto(Tenant tenant) {
		this.id = tenant.getId();
		this.name = tenant.getName();
		this.slug = tenant.getSlug();
		this.active = tenant.isActive();
		this.createdAt = tenant.getCreatedAt();
		this.logoUrl = tenant.getLogoBytes() != null && tenant.getLogoBytes().length > 0
				? "/api/public/tenants/" + tenant.getSlug() + "/logo"
				: null;
		this.socialLink = tenant.getSocialLink() != null ? new SocialLinkDto(tenant.getSocialLink()) : null;
		this.features = tenant.getFeatures() != null ? new HashSet<>(tenant.getFeatures()) : new HashSet<>();
		this.accentColor = tenant.getAccentColor();
		this.backgroundColor = tenant.getBackgroundColor();
	}
}

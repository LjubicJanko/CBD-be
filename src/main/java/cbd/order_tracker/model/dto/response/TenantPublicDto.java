package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.dto.SocialLinkDto;
import lombok.Data;

@Data
public class TenantPublicDto {
	private String name;
	private String slug;
	private String logoUrl;
	private SocialLinkDto socialLink;

	public TenantPublicDto(Tenant tenant) {
		this.name = tenant.getName();
		this.slug = tenant.getSlug();
		this.logoUrl = tenant.getLogoBytes() != null && tenant.getLogoBytes().length > 0
				? "/api/public/tenants/" + tenant.getSlug() + "/logo"
				: null;
		this.socialLink = tenant.getSocialLink() != null ? new SocialLinkDto(tenant.getSocialLink()) : null;
	}
}

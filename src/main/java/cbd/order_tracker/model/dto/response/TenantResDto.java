package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.Tenant;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantResDto {
	private Long id;
	private String name;
	private String slug;
	private boolean active;
	private LocalDateTime createdAt;
	private String logoUrl;

	public TenantResDto(Tenant tenant) {
		this.id = tenant.getId();
		this.name = tenant.getName();
		this.slug = tenant.getSlug();
		this.active = tenant.isActive();
		this.createdAt = tenant.getCreatedAt();
		this.logoUrl = tenant.getLogoBytes() != null && tenant.getLogoBytes().length > 0
				? "/api/public/tenants/" + tenant.getSlug() + "/logo"
				: null;
	}
}

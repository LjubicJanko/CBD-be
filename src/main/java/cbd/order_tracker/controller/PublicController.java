package cbd.order_tracker.controller;

import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.dto.response.TenantPublicDto;
import cbd.order_tracker.service.PlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

	private final PlatformService platformService;

	@GetMapping("/tenants/{slug}")
	public ResponseEntity<TenantPublicDto> getTenant(@PathVariable String slug) {
		return ResponseEntity.ok(platformService.getPublicTenant(slug));
	}

	@GetMapping("/tenants/{slug}/logo")
	public ResponseEntity<byte[]> getTenantLogo(@PathVariable String slug) {
		Tenant tenant = platformService.getTenantBySlug(slug);
		byte[] bytes = tenant.getLogoBytes();
		if (bytes == null || bytes.length == 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logo not set");
		}
		String contentType = tenant.getLogoContentType() != null ? tenant.getLogoContentType() : MediaType.IMAGE_PNG_VALUE;
		return ResponseEntity.ok()
				.header("Content-Type", contentType)
				.header("Cache-Control", "public, max-age=60")
				.body(bytes);
	}
}

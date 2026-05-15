package cbd.order_tracker.service;

import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.RegisterUserDto;
import cbd.order_tracker.model.dto.request.CreateTenantReqDto;
import cbd.order_tracker.model.dto.response.TenantPublicDto;
import cbd.order_tracker.model.dto.response.TenantResDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PlatformService {

	List<TenantResDto> getAllTenants();

	TenantResDto getTenantById(Long id);

	TenantResDto createTenant(CreateTenantReqDto dto);

	TenantResDto updateTenant(Long id, CreateTenantReqDto dto);

	void deactivateTenant(Long id);

	User createUserForTenant(Long tenantId, RegisterUserDto dto);

	TenantResDto uploadLogo(Long tenantId, MultipartFile file);

	void deleteLogo(Long tenantId);

	TenantPublicDto getPublicTenant(String slug);

	Tenant getTenantBySlug(String slug);
}

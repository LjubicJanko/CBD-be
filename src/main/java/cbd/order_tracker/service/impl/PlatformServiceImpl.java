package cbd.order_tracker.service.impl;

import cbd.order_tracker.exceptions.RoleNotFoundException;
import cbd.order_tracker.exceptions.TenantNotFoundException;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.User;
import cbd.order_tracker.model.dto.RegisterUserDto;
import cbd.order_tracker.model.dto.SocialLinkDto;
import cbd.order_tracker.model.dto.request.CreateTenantReqDto;
import cbd.order_tracker.model.dto.request.UpdateOwnTenantReqDto;
import cbd.order_tracker.model.dto.response.TenantPublicDto;
import cbd.order_tracker.model.dto.response.TenantResDto;
import cbd.order_tracker.repository.RolesRepository;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.repository.UserRepository;
import cbd.order_tracker.service.PlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformServiceImpl implements PlatformService {

	private static final long MAX_LOGO_BYTES = 1024L * 1024L;
	private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

	// Slugs that would collide with FE routes or known API path segments.
	private static final java.util.Set<String> RESERVED_SLUGS = java.util.Set.of(
			"platform", "login", "dashboard", "profile", "reports", "track",
			"select-tenant", "order-extension", "api", "public", "banners",
			"orders", "orderextend", "auth", "assets", "static"
	);

	private final TenantRepository tenantRepository;
	private final UserRepository userRepository;
	private final RolesRepository rolesRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public List<TenantResDto> getAllTenants() {
		return tenantRepository.findAll().stream()
				.map(TenantResDto::new)
				.collect(Collectors.toList());
	}

	@Override
	public TenantResDto getTenantById(Long id) {
		Tenant tenant = tenantRepository.findById(id)
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		return new TenantResDto(tenant);
	}

	@Override
	@Transactional
	public TenantResDto createTenant(CreateTenantReqDto dto) {
		String normalizedSlug = dto.getSlug().toLowerCase();
		if (RESERVED_SLUGS.contains(normalizedSlug)) {
			throw new IllegalArgumentException("Slug '" + normalizedSlug + "' is reserved");
		}
		if (tenantRepository.findBySlug(normalizedSlug).isPresent()) {
			throw new IllegalArgumentException("Tenant with slug '" + normalizedSlug + "' already exists");
		}
		Tenant tenant = new Tenant(dto.getName(), normalizedSlug);
		if (dto.getSocialLink() != null) {
			tenant.setSocialLink(dto.getSocialLink().toEntity());
		}
		tenant = tenantRepository.save(tenant);
		return new TenantResDto(tenant);
	}

	@Override
	@Transactional
	public TenantResDto updateTenant(Long id, CreateTenantReqDto dto) {
		Tenant tenant = tenantRepository.findById(id)
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		tenant.setName(dto.getName());
		if (dto.getSlug() != null) {
			String normalizedSlug = dto.getSlug().toLowerCase();
			if (RESERVED_SLUGS.contains(normalizedSlug)) {
				throw new IllegalArgumentException("Slug '" + normalizedSlug + "' is reserved");
			}
			if (!normalizedSlug.equals(tenant.getSlug())
					&& tenantRepository.findBySlug(normalizedSlug).isPresent()) {
				throw new IllegalArgumentException("Tenant with slug '" + normalizedSlug + "' already exists");
			}
			tenant.setSlug(normalizedSlug);
		}
		applySocialLink(tenant, dto.getSocialLink(), dto.isSocialLinkProvided());
		tenant = tenantRepository.save(tenant);
		return new TenantResDto(tenant);
	}

	@Override
	@Transactional
	public TenantResDto updateOwnTenant(Long tenantId, UpdateOwnTenantReqDto dto) {
		Tenant tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		// Self-service: name + socialLink only. Slug and active are intentionally not touched.
		tenant.setName(dto.getName());
		applySocialLink(tenant, dto.getSocialLink(), dto.isSocialLinkProvided());
		tenant = tenantRepository.save(tenant);
		return new TenantResDto(tenant);
	}

	// Null-vs-omitted: socialLink explicitly present (even as null) is applied — a non-null
	// object sets/replaces, an explicit null clears; an omitted field (not provided) leaves
	// the existing link untouched.
	private void applySocialLink(Tenant tenant, SocialLinkDto socialLink, boolean provided) {
		if (provided) {
			tenant.setSocialLink(socialLink != null ? socialLink.toEntity() : null);
		}
	}

	@Override
	@Transactional
	public void deactivateTenant(Long id) {
		Tenant tenant = tenantRepository.findById(id)
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		tenant.setActive(false);
		tenantRepository.save(tenant);
	}

	@Override
	@Transactional
	public User createUserForTenant(Long tenantId, RegisterUserDto dto) {
		Tenant tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		var role = rolesRepository.findByNameWithPrivileges(dto.getRole())
				.orElseThrow(() -> new RoleNotFoundException("Role not found"));
		var user = new User(dto, role, passwordEncoder.encode(dto.getPassword()), tenant);
		return userRepository.save(user);
	}

	@Override
	@Transactional
	public TenantResDto uploadLogo(Long tenantId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Logo file is required");
		}
		if (file.getSize() > MAX_LOGO_BYTES) {
			throw new IllegalArgumentException("Logo exceeds 1 MB limit");
		}
		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not read uploaded file");
		}
		if (!isPng(bytes)) {
			throw new IllegalArgumentException("Only PNG images are accepted");
		}
		Tenant tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		tenant.setLogoBytes(bytes);
		tenant.setLogoContentType("image/png");
		tenant = tenantRepository.save(tenant);
		return new TenantResDto(tenant);
	}

	@Override
	@Transactional
	public void deleteLogo(Long tenantId) {
		Tenant tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		tenant.setLogoBytes(null);
		tenant.setLogoContentType(null);
		tenantRepository.save(tenant);
	}

	@Override
	public TenantPublicDto getPublicTenant(String slug) {
		Tenant tenant = tenantRepository.findBySlug(slug.toLowerCase())
				.filter(Tenant::isActive)
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
		return new TenantPublicDto(tenant);
	}

	@Override
	public Tenant getTenantBySlug(String slug) {
		return tenantRepository.findBySlug(slug.toLowerCase())
				.filter(Tenant::isActive)
				.orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
	}

	private static boolean isPng(byte[] bytes) {
		if (bytes.length < PNG_MAGIC.length) return false;
		for (int i = 0; i < PNG_MAGIC.length; i++) {
			if (bytes[i] != PNG_MAGIC[i]) return false;
		}
		return true;
	}
}

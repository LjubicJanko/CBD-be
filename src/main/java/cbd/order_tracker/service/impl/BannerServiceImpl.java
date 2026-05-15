package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.config.TenantGuard;
import cbd.order_tracker.exceptions.BannerNotFoundException;
import cbd.order_tracker.exceptions.TenantNotFoundException;
import cbd.order_tracker.model.Banner;
import cbd.order_tracker.model.BannerPlacement;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.dto.request.BannerReqDto;
import cbd.order_tracker.model.dto.response.BannerResDto;
import cbd.order_tracker.model.enums.PageType;
import cbd.order_tracker.repository.BannerPlacementRepository;
import cbd.order_tracker.repository.BannerRepository;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.service.BannerService;
import cbd.order_tracker.util.BannerMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final BannerPlacementRepository placementRepository;
    private final TenantRepository tenantRepository;

    private Long tenantId() {
        return TenantContext.requireTenantId();
    }

    private Banner findBannerForCurrentTenant(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new BannerNotFoundException("Banner not found"));
        TenantGuard.assertTenantMatch(banner.getTenant());
        return banner;
    }

    @Override
    public List<BannerResDto> getAll() {
        List<Banner> banners = bannerRepository.findAllWithPlacements(TenantContext.requireTenantId());
        return BannerMapper.toDtoList(banners);
    }

    @Override
    public void deleteBanner(Long bannerId) {
        Banner banner = findBannerForCurrentTenant(bannerId);
        banner.setDeleted(true);
        bannerRepository.save(banner);
    }

    @Override
    public BannerResDto getPublished() {
        Banner banner = bannerRepository.findByPlacements_ActiveTrue(TenantContext.requireTenantId()).stream()
                .findFirst()
                .orElseThrow(() -> new BannerNotFoundException("No active banner found"));
        return new BannerResDto(banner);
    }

    @Transactional
    @Override
    public BannerResDto getActiveBanner(String tenantSlug) {
        Tenant tenant = tenantRepository.findBySlug(tenantSlug.toLowerCase()).orElse(null);
        if (tenant == null || !tenant.isActive()) {
            return null;
        }
        List<BannerPlacement> placements = placementRepository.findAllByActiveTrueWithBanner(tenant.getId());

        if (placements.isEmpty()) {
            return null;
        }

        Banner activeBanner = placements.get(0).getBanner();

        List<PageType> activePages = placements.stream()
                .filter(p -> p.getBanner().getId().equals(activeBanner.getId()))
                .map(BannerPlacement::getPage)
                .toList();

        BannerResDto dto = new BannerResDto(activeBanner);
        dto.setActivePages(activePages);

        return dto;
    }

    @Transactional
    @Override
    public BannerResDto publish(Long bannerId, List<PageType> pages) {
        // Unpublish all currently active placements for this tenant
        placementRepository.findAllByActiveTrue(tenantId()).forEach(p -> p.setActive(false));

        Banner banner = findBannerForCurrentTenant(bannerId);

        placementRepository.deleteByBanner_Id(bannerId);

        for (PageType page : pages) {
            BannerPlacement placement = BannerPlacement.builder()
                    .banner(banner)
                    .page(page)
                    .active(true)
                    .build();
            placementRepository.save(placement);
        }

        return new BannerResDto(banner);
    }

    @Transactional
    @Override
    public void unpublish(Long bannerId) {
        findBannerForCurrentTenant(bannerId);
        placementRepository.findByBanner_Id(bannerId)
                .forEach(p -> p.setActive(false));
        placementRepository.deleteByBanner_Id(bannerId);
    }

    @Override
    public BannerResDto createBanner(BannerReqDto bannerReqDto) {
        Banner banner = new Banner(bannerReqDto);
        Tenant tenant = tenantRepository.findById(tenantId())
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
        banner.setTenant(tenant);
        banner = bannerRepository.save(banner);
        return new BannerResDto(banner);
    }

    @Override
    public BannerResDto editBanner(Long id, BannerReqDto bannerReqDto) {
        Banner banner = findBannerForCurrentTenant(id);

        banner.setTitle(bannerReqDto.getTitle());
        banner.setText(bannerReqDto.getText());

        banner = bannerRepository.save(banner);
        return new BannerResDto(banner);
    }
}

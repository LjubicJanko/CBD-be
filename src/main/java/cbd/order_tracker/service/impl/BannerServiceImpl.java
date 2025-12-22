package cbd.order_tracker.service.impl;

import cbd.order_tracker.model.Banner;
import cbd.order_tracker.model.BannerPlacement;
import cbd.order_tracker.model.dto.request.BannerReqDto;
import cbd.order_tracker.model.dto.response.BannerResDto;
import cbd.order_tracker.model.enums.PageType;
import cbd.order_tracker.repository.BannerPlacementRepository;
import cbd.order_tracker.repository.BannerRepository;
import cbd.order_tracker.service.BannerService;
import cbd.order_tracker.util.BannerMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final BannerPlacementRepository placementRepository;

    @Override
    public List<BannerResDto> getAll() {
        List<Banner> banners = bannerRepository.findAllWithPlacements();
        return BannerMapper.toDtoList(banners);
    }

    @Override
    public void deleteBanner(Long bannerId) {
        Banner banner = bannerRepository.findById(bannerId).orElseThrow(() -> new RuntimeException("Banner not found"));
        banner.setDeleted(true);
        bannerRepository.save(banner);
    }

    @Override
    public BannerResDto getPublished() {
        Banner banner = bannerRepository.findByPlacements_ActiveTrue()
                .orElseThrow(() -> new RuntimeException("No active banner found"));
        return new BannerResDto(banner);
    }

    @Transactional
    @Override
    public BannerResDto getActiveBanner() {
        List<BannerPlacement> placements = placementRepository.findAllByActiveTrueWithBanner();

        if (placements.isEmpty()) {
            throw new RuntimeException("No active banner found");
        }

        // Assume only one banner can be active at a time
        Banner activeBanner = placements.get(0).getBanner();

        // Collect all pages where this banner is active
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
        // Unpublish all currently active placements
        placementRepository.findAll().forEach(p -> p.setActive(false));

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new RuntimeException("Banner not found"));

        // Remove old placements
        placementRepository.deleteByBanner_Id(bannerId);

        // Add new active placements
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
        placementRepository.findByBanner_Id(bannerId)
                .forEach(p -> p.setActive(false));
        placementRepository.deleteByBanner_Id(bannerId);
    }

    @Override
    public BannerResDto createBanner(BannerReqDto bannerReqDto) {
        Banner banner = new Banner(bannerReqDto);
        banner = bannerRepository.save(banner);
        return new BannerResDto(banner);
    }

    @Override
    public BannerResDto editBanner(Long id, BannerReqDto bannerReqDto) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No active banner found"));

        banner.setTitle(bannerReqDto.getTitle());
        banner.setText(bannerReqDto.getText());

        banner = bannerRepository.save(banner);
        return new BannerResDto(banner);
    }
}

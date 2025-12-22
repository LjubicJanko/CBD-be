package cbd.order_tracker.service;

import cbd.order_tracker.model.dto.request.BannerReqDto;
import cbd.order_tracker.model.dto.response.BannerResDto;
import cbd.order_tracker.model.enums.PageType;

import java.util.List;

public interface BannerService {

    List<BannerResDto> getAll();

    void deleteBanner(Long bannerId);

    BannerResDto getPublished();

    BannerResDto getActiveBanner();

    BannerResDto publish(Long bannerId, List<PageType> pages);

    void unpublish(Long bannerId);

    BannerResDto createBanner(BannerReqDto banner);

    BannerResDto editBanner(Long id, BannerReqDto bannerReqDto);
}

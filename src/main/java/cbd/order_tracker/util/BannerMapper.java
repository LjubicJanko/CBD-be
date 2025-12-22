package cbd.order_tracker.util;

import cbd.order_tracker.model.Banner;
import cbd.order_tracker.model.dto.response.BannerResDto;

import java.util.List;

public class BannerMapper {

    public static List<BannerResDto> toDtoList(List<Banner> banners) {
        return banners.stream().map(
                BannerResDto::new
        ).toList();
    }
}

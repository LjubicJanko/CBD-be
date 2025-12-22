package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.Banner;
import cbd.order_tracker.model.BannerPlacement;
import cbd.order_tracker.model.enums.PageType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BannerResDto {
    private Long id;
    private String title;
    private String text;
    private List<PageType> activePages;

    public BannerResDto(Banner banner) {
        this.setId(banner.getId());
        this.setTitle(banner.getTitle());
        this.setText(banner.getText());
        if(banner.getPlacements().isEmpty()) {
            this.activePages = new ArrayList<>();
        } else {
            this.activePages = banner.getPlacements().stream()
                .filter(BannerPlacement::isActive)
                .map(BannerPlacement::getPage)
                .toList();
        }
    }
}

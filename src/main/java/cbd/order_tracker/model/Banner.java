package cbd.order_tracker.model;

import cbd.order_tracker.model.dto.request.BannerReqDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@SQLRestriction("deleted = false")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String text;

    @OneToMany(mappedBy = "banner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BannerPlacement> placements = new ArrayList<>();

    private boolean deleted = false;

    public Banner(BannerReqDto bannerReqDto) {
        this.setTitle(bannerReqDto.getTitle());
        this.setText(bannerReqDto.getText());
        this.deleted = false;
    }
}

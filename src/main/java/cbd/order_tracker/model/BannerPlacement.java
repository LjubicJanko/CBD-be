package cbd.order_tracker.model;

import cbd.order_tracker.model.enums.PageType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerPlacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_id")
    private Banner banner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PageType page;

    private boolean active = false;
}

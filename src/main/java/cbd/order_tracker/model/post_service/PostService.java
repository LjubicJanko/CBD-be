package cbd.order_tracker.model.post_service;

import cbd.order_tracker.model.dto.request.PostServiceReqDto;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_service")
@Data
@NoArgsConstructor
public class PostService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String websiteUrl;

    public PostService(PostServiceReqDto postServiceReqDto) {
        this.id = postServiceReqDto.getId();
        this.name = postServiceReqDto.getName();
        this.websiteUrl = postServiceReqDto.getWebsiteUrl();
    }
}

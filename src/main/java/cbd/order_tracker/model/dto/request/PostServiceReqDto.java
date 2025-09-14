package cbd.order_tracker.model.dto.request;


import cbd.order_tracker.model.post_service.PostService;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PostServiceReqDto {
    private Long id;
    private String name;
    private String websiteUrl;

    public PostServiceReqDto(PostService postService) {
        this.id = postService.getId();
        this.name = postService.getName();
        this.websiteUrl = postService.getWebsiteUrl();
    }
}

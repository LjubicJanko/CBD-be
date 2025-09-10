package cbd.order_tracker.model.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GearReqDto {
    private Long id;
    private String name;
    private Long categoryId;
    private Long typeId;
}

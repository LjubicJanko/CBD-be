package cbd.order_tracker.model.dto.request;

import cbd.order_tracker.model.ContactInfo;
import lombok.Data;

import java.util.List;

@Data
public class CombineExtensionsReqDto {
    private List<Long> extensionIds;
    private String name;
    private String description;
    private ContactInfo contactInfo;
}

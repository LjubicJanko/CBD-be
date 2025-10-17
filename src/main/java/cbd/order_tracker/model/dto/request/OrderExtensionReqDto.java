package cbd.order_tracker.model.dto.request;

import cbd.order_tracker.model.ContactInfo;
import lombok.Data;

@Data
public class OrderExtensionReqDto {
    private String name;
    private String description;
    private ContactInfo contactInfo;
}

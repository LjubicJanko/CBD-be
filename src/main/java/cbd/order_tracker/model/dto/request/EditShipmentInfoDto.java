package cbd.order_tracker.model.dto.request;

import lombok.Data;

@Data
public class EditShipmentInfoDto {
    private String postalService;
    private String postalCode;
}

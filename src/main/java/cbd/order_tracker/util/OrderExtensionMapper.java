package cbd.order_tracker.util;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.dto.response.OrderExtensionDto;

public class OrderExtensionMapper {


    public static OrderExtensionDto toDto(OrderRecord orderRecord) {
        OrderExtensionDto dto = new OrderExtensionDto();

        dto.setId(orderRecord.getId());
        dto.setName(orderRecord.getName());
        dto.setDescription(orderRecord.getDescription());
        dto.setStatus(orderRecord.getStatus());
        dto.setExecutionStatus(orderRecord.getExecutionStatus());
        dto.setTrackingId(orderRecord.getTrackingId());
        dto.setExtension(orderRecord.getExtension());
        dto.setContactInfo(orderRecord.getContactInfo());
        return dto;
    }
}

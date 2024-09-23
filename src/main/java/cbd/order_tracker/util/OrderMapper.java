package cbd.order_tracker.util;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.dto.OrderDTO;

public class OrderMapper {

    public static OrderDTO toDto(OrderRecord orderRecord) {
        OrderDTO dto = new OrderDTO();
        dto.setId(orderRecord.getId());
        dto.setName(orderRecord.getName());
        dto.setDescription(orderRecord.getDescription());
        dto.setStatus(orderRecord.getStatus());
        dto.setTrackingId(orderRecord.getTrackingId());
        dto.setStatusHistory(orderRecord.getStatusHistory());

        return dto;
    }
}

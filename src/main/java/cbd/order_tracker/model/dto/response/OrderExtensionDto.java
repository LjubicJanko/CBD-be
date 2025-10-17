package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.*;
import cbd.order_tracker.model.dto.OrderStatusHistoryDTO;
import lombok.Data;

import java.util.List;

@Data
public class OrderExtensionDto {
    private Long id;
    private String name;
    private String description;
    private OrderStatus status;
    private OrderExecutionStatus executionStatus;
    private String trackingId;
    private List<OrderStatusHistoryDTO> statusHistory;
    private List<Payment> payments;
    private ContactInfo contactInfo;
    private Boolean extension;
}

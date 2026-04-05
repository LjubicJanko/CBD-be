package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatusDurationDto {
    private OrderStatus status;
    private Double averageHours;
    private Double percentage;
}

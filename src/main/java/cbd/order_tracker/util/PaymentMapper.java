package cbd.order_tracker.util;

import cbd.order_tracker.model.Payment;
import cbd.order_tracker.model.dto.PaymentDto;

public class PaymentMapper {
    public static PaymentDto toDto(Payment payment) {
        return new PaymentDto(payment);
    }
}

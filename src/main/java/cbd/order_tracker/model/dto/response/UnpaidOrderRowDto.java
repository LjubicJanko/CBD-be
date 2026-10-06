package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UnpaidOrderRowDto(
        Long orderId,
        String orderName,
        String trackingId,
        OrderStatus status,
        OrderExecutionStatus executionStatus,
        BigDecimal salePrice,
        BigDecimal amountPaid,
        BigDecimal amountLeftToPay,
        LocalDate lastPaymentDate,
        Long paymentCount
) {
}

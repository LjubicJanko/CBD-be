package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentReportRowDto(
        Long id,
        Long orderId,
        String orderName,
        String trackingId,
        String payer,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod paymentMethod,
        String note
) {
}

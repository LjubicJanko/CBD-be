package cbd.order_tracker.model.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentsTotalsDto(
        BigDecimal overall,
        long count,
        BigDecimal bankTotal,
        Map<String, BigDecimal> byMethod
) {
}

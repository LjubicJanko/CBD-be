package cbd.order_tracker.model.dto.response;

import java.math.BigDecimal;

public record UnpaidTotalsDto(BigDecimal outstanding, long count) {
}

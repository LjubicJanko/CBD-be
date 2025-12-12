package cbd.order_tracker.util;

import java.math.BigDecimal;

public class CommonHelper {

    public static BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
        return (a == null ? BigDecimal.ZERO : a)
                .subtract(b == null ? BigDecimal.ZERO : b);
    }
}

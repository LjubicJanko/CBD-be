package cbd.order_tracker.model.dto.response;

import java.math.BigDecimal;

public record OrderReportRaw(
        long orderCount,
        BigDecimal totalAcquisitionCost,
        BigDecimal avgAcquisitionCost,
        BigDecimal totalAmountPaid,
        BigDecimal totalSalePrice,
        BigDecimal totalOutstanding,
        long extensionOrderCount,
        long regularOrderCount
) {
    public static OrderReportRaw from(Object[] raw) {
        Object[] r = (raw.length == 1 && raw[0] instanceof Object[]) ? (Object[]) raw[0] : raw;
        return new OrderReportRaw(
                ((Number) r[0]).longValue(),
                new BigDecimal(r[1].toString()),
                new BigDecimal(r[2].toString()),
                new BigDecimal(r[3].toString()),
                new BigDecimal(r[4].toString()),
                new BigDecimal(r[5].toString()),
                ((Number) r[6]).longValue(),
                ((Number) r[7]).longValue()
        );
    }
}

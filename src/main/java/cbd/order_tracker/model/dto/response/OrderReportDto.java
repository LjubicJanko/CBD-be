package cbd.order_tracker.model.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderReportDto {

    // visible to all roles
    private Long orderCount;
    private BigDecimal totalAcquisitionCost;
    private BigDecimal averageAcquisitionCost;

    // admin only
    private BigDecimal totalAmountPaid;
    private BigDecimal totalSalePrice;
    private BigDecimal totalOutstanding;
    private BigDecimal profitMargin;
}

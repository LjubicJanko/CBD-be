package cbd.order_tracker.util;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.OrderOverviewDto;
import cbd.order_tracker.model.dto.OrderTrackingDTO;

import java.math.BigDecimal;

public class OrderMapper {

    public static OrderDTO toDto(OrderRecord orderRecord) {
        OrderDTO dto = new OrderDTO();
        dto.setId(orderRecord.getId());
        dto.setName(orderRecord.getName());
        dto.setDescription(orderRecord.getDescription());
        dto.setStatus(orderRecord.getStatus());
        dto.setExecutionStatus(orderRecord.getExecutionStatus());
        dto.setPausingComment(orderRecord.getPausingComment());
        dto.setTrackingId(orderRecord.getTrackingId());
        dto.setStatusHistory(orderRecord.getStatusHistory());
        dto.setAcquisitionCost(orderRecord.getAcquisitionCost());
        dto.setAmountPaid(orderRecord.getAmountPaid());
        dto.setLegalEntity(orderRecord.isLegalEntity());

        BigDecimal salePrice = orderRecord.isLegalEntity() ? orderRecord.getSalePriceWithTax() : orderRecord.getSalePrice();
        BigDecimal priceDifference = salePrice.subtract(orderRecord.getAcquisitionCost());
        BigDecimal amountLefToPay = priceDifference.subtract(orderRecord.getAmountPaid());

        dto.setSalePrice(salePrice);
        dto.setPriceDifference(orderRecord.getPriceDifference());
        dto.setAmountLeftToPay(amountLefToPay);
        dto.setPlannedEndingDate(orderRecord.getPlannedEndingDate());
        dto.setPayments(orderRecord.getPayments());

        return dto;
    }

    public static OrderOverviewDto toOverviewDto(OrderRecord orderRecord) {
        return new OrderOverviewDto(orderRecord);
    }

    public static OrderTrackingDTO toOrderTrackingDTO(OrderRecord orderRecord) {
        return new OrderTrackingDTO(orderRecord);
    }
}

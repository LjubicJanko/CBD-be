package cbd.order_tracker.util;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.OrderOverviewDto;
import cbd.order_tracker.model.dto.OrderTrackingDTO;

public class OrderMapper {

    public static OrderDTO toDto(OrderRecord orderRecord) {
        OrderDTO dto = new OrderDTO();
        dto.setId(orderRecord.getId());
        dto.setName(orderRecord.getName());
        dto.setDescription(orderRecord.getDescription());
        dto.setStatus(orderRecord.getStatus());
        dto.setTrackingId(orderRecord.getTrackingId());
        dto.setStatusHistory(orderRecord.getStatusHistory());
        dto.setAcquisitionCost(orderRecord.getAcquisitionCost());
        dto.setAmountLeftToPay(orderRecord.getAmountLeftToPay());
        dto.setAmountPaid(orderRecord.getAmountPaid());
        dto.setAmountLeftToPayWithTax(orderRecord.getAmountLeftToPayWithTax());
        dto.setLegalEntity(orderRecord.isLegalEntity());
        dto.setPriceDifference(orderRecord.getPriceDifference());
        dto.setSalePrice(orderRecord.getSalePrice());
        dto.setPlannedEndingDate(orderRecord.getPlannedEndingDate());
        dto.setSalePriceWithTax(orderRecord.getSalePriceWithTax());
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

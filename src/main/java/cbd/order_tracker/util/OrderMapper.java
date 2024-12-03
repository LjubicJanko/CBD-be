package cbd.order_tracker.util;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.OrderOverviewDto;
import cbd.order_tracker.model.dto.OrderTrackingDTO;

import java.math.BigDecimal;
import java.util.Collection;

public class OrderMapper {

	public static OrderDTO toDto(OrderRecord orderRecord) {
		OrderDTO dto = new OrderDTO();
		Collection<Role> roles = UserUtil.getCurrentUserRoles();
		Boolean isAdmin = roles.stream().anyMatch(role -> "admin".equals(role.getName()));

		dto.setId(orderRecord.getId());
		dto.setName(orderRecord.getName());
		dto.setDescription(orderRecord.getDescription());
		dto.setNote(orderRecord.getNote());
		dto.setStatus(orderRecord.getStatus());
		dto.setExecutionStatus(orderRecord.getExecutionStatus());
		dto.setPausingComment(orderRecord.getPausingComment());
		dto.setTrackingId(orderRecord.getTrackingId());
		dto.setStatusHistory(orderRecord.getStatusHistory());
		dto.setAcquisitionCost(orderRecord.getAcquisitionCost());
		dto.setLegalEntity(orderRecord.isLegalEntity());
		dto.setPlannedEndingDate(orderRecord.getPlannedEndingDate());
		BigDecimal priceForCalculation = orderRecord.isLegalEntity() ? orderRecord.getSalePriceWithTax() : orderRecord.getSalePrice();
		BigDecimal priceDifference = priceForCalculation.subtract(orderRecord.getAcquisitionCost());
		BigDecimal amountLefToPay = priceForCalculation.subtract(orderRecord.getAmountPaid());
		dto.setAmountLeftToPay(amountLefToPay);

		if (isAdmin) {

			dto.setAmountPaid(orderRecord.getAmountPaid());
			dto.setSalePrice(orderRecord.getSalePrice());
			dto.setSalePriceWithTax(orderRecord.getSalePriceWithTax());
			dto.setPriceDifference(priceDifference);
			dto.setPayments(orderRecord.getPayments());
		}

		return dto;
	}

	public static OrderOverviewDto toOverviewDto(OrderRecord orderRecord) {
		return new OrderOverviewDto(orderRecord);
	}

	public static OrderTrackingDTO toOrderTrackingDTO(OrderRecord orderRecord) {
		return new OrderTrackingDTO(orderRecord);
	}
}

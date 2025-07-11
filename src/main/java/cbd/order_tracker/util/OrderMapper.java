package cbd.order_tracker.util;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatusHistory;
import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.OrderOverviewDto;
import cbd.order_tracker.model.dto.OrderStatusHistoryDTO;
import cbd.order_tracker.model.dto.OrderTrackingDTO;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public class OrderMapper {

	public static List<OrderStatusHistoryDTO> mapStatusHistory(List<OrderStatusHistory> history) {
		return history.stream()
				.map(h -> {
					OrderStatusHistoryDTO hDto = new OrderStatusHistoryDTO();
					hDto.setId(h.getId());
					hDto.setStatus(h.getStatus());
					hDto.setClosingComment(h.getClosingComment());
					hDto.setCreationTime(h.getCreationTime());
					hDto.setPostalCode(h.getPostalCode());
					hDto.setPostalService(h.getPostalService());
					if (h.getUser() != null) {
						hDto.setUser(h.getUser());
					}
					return hDto;
				})
				.toList();
	}


	public static OrderDTO toDto(OrderRecord orderRecord, List<OrderStatusHistory> history, Collection<Role> roles) {
		OrderDTO dto = new OrderDTO();
		boolean isAdmin = roles.stream().anyMatch(role -> "admin".equals(role.getName()));

		dto.setId(orderRecord.getId());
		dto.setName(orderRecord.getName());
		dto.setDescription(orderRecord.getDescription());
		dto.setNote(orderRecord.getNote());
		dto.setStatus(orderRecord.getStatus());
		dto.setPriority(orderRecord.getPriority());
		dto.setExecutionStatus(orderRecord.getExecutionStatus());
		dto.setPausingComment(orderRecord.getPausingComment());
		dto.setTrackingId(orderRecord.getTrackingId());
		dto.setStatusHistory(mapStatusHistory(history));
		dto.setPostalCode(orderRecord.getPostalCode());
		dto.setPostalService(orderRecord.getPostalService());

		dto.setAcquisitionCost(orderRecord.getAcquisitionCost());
		dto.setLegalEntity(orderRecord.isLegalEntity());
		dto.setPlannedEndingDate(orderRecord.getPlannedEndingDate());
		BigDecimal priceForCalculation = orderRecord.isLegalEntity() ? orderRecord.getSalePriceWithTax() : orderRecord.getSalePrice();
		BigDecimal priceDifference = orderRecord.getSalePrice().subtract(orderRecord.getAcquisitionCost());
		BigDecimal amountLefToPay = priceForCalculation.subtract(orderRecord.getAmountPaid());
		dto.setAmountLeftToPay(amountLefToPay);

		if (isAdmin) {

			dto.setAmountPaid(orderRecord.getAmountPaid());
			dto.setSalePrice(orderRecord.getSalePrice());
			dto.setSalePriceWithTax(orderRecord.getSalePriceWithTax());
			dto.setPriceDifference(priceDifference);
//			dto.setPayments(orderRecord.getPayments());
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

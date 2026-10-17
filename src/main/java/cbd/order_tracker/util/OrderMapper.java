package cbd.order_tracker.util;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatusHistory;
import cbd.order_tracker.model.Payment;
import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.OrderOverviewDto;
import cbd.order_tracker.model.dto.OrderStatusHistoryDTO;
import cbd.order_tracker.model.dto.OrderTrackingDTO;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class OrderMapper {

	public static List<OrderStatusHistoryDTO> mapStatusHistory(List<OrderStatusHistory> history) {
		Map<OrderStatusHistory, OrderExecutionStatus> previous = derivePreviousExecutionStatuses(history);
		return history.stream()
				.map(h -> {
					OrderStatusHistoryDTO hDto = new OrderStatusHistoryDTO();
					hDto.setId(h.getId());
					hDto.setStatus(h.getStatus());
					hDto.setExecutionStatus(h.getExecutionStatus());
					hDto.setPreviousExecutionStatus(previous.get(h));
					hDto.setClosingComment(h.getClosingComment());
					hDto.setCreationTime(h.getCreationTime());
					hDto.setPostalCode(h.getPostalCode());
					hDto.setPostalService(h.getPostalService());
					hDto.setPrintFilesUrl(h.getPrintFilesUrl());
					hDto.setEventType(h.getEventType());
					hDto.setRelatedOrderNames(h.getRelatedOrderNames());
					if (h.getUser() != null) {
						hDto.setUser(h.getUser());
					}
					return hDto;
				})
				.toList();
	}


	/**
	 * For each execution-status row (non-null executionStatus), the executionStatus of the nearest earlier
	 * execution-status row (ordered by creationTime, then id), or ACTIVE for the first one.
	 * Keyed by identity; rows that are not execution-status rows are absent.
	 */
	private static Map<OrderStatusHistory, OrderExecutionStatus> derivePreviousExecutionStatuses(List<OrderStatusHistory> history) {
		Map<OrderStatusHistory, OrderExecutionStatus> result = new IdentityHashMap<>();
		List<OrderStatusHistory> executionRows = history.stream()
				.filter(h -> h.getExecutionStatus() != null)
				.sorted(Comparator
						.comparing(OrderStatusHistory::getCreationTime, Comparator.nullsFirst(Comparator.naturalOrder()))
						.thenComparing(OrderStatusHistory::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
				.toList();
		OrderExecutionStatus prev = OrderExecutionStatus.ACTIVE;
		for (OrderStatusHistory row : executionRows) {
			result.put(row, prev);
			prev = row.getExecutionStatus();
		}
		return result;
	}

	public static OrderDTO toDto(OrderRecord orderRecord, List<OrderStatusHistory> history, Collection<Role> roles) {
		return toDto(orderRecord, history, roles, null);
	}

	public static OrderDTO toDto(OrderRecord orderRecord, List<OrderStatusHistory> history, Collection<Role> roles, List<Payment> payments) {
		OrderDTO dto = new OrderDTO();
		boolean isAdmin = isAdmin(roles);

		dto.setId(orderRecord.getId());
		dto.setName(orderRecord.getName());
		dto.setDescription(orderRecord.getDescription());
		boolean internalNote = Boolean.TRUE.equals(orderRecord.getInternalNote());
		if (isAdmin) {
			dto.setNote(orderRecord.getNote());
			dto.setInternalNote(internalNote);
		} else if (!internalNote) {
			// "" (never null) so clients can tell an empty note from a hidden internal one
			dto.setNote(orderRecord.getNote() != null ? orderRecord.getNote() : "");
		}
		dto.setStatus(orderRecord.getStatus());
		dto.setPriority(orderRecord.getPriority());
		dto.setExecutionStatus(orderRecord.getExecutionStatus());
		dto.setPausingComment(orderRecord.getPausingComment());
		dto.setTrackingId(orderRecord.getTrackingId());
		dto.setStatusHistory(mapStatusHistory(history));
		dto.setPostalCode(orderRecord.getPostalCode());
		dto.setPostalService(orderRecord.getPostalService());
		dto.setPrintFilesUrl(orderRecord.getPrintFilesUrl());

		dto.setAcquisitionCost(orderRecord.getAcquisitionCost());
		dto.setLegalEntity(orderRecord.isLegalEntity());
		dto.setPlannedEndingDate(orderRecord.getPlannedEndingDate());
		BigDecimal priceForCalculation = orderRecord.isLegalEntity() ? orderRecord.getSalePriceWithTax() : orderRecord.getSalePrice();
		BigDecimal priceDifference = orderRecord.getSalePrice().subtract(orderRecord.getAcquisitionCost());
		BigDecimal amountLefToPay = priceForCalculation.subtract(orderRecord.getAmountPaid());
		dto.setAmountLeftToPay(amountLefToPay);
		dto.setExtension(orderRecord.getExtension());

		if (isAdmin) {

			dto.setAmountPaid(orderRecord.getAmountPaid());
			dto.setSalePrice(orderRecord.getSalePrice());
			dto.setSalePriceWithTax(orderRecord.getSalePriceWithTax());
			dto.setPriceDifference(priceDifference);
			dto.setContactInfo(orderRecord.getContactInfo());
		}

		if (payments != null) {
			dto.setPayments(payments);
		}

		return dto;
	}

	public static boolean isAdmin(Collection<Role> roles) {
		return TenantContext.isSuperadmin() || roles.stream().anyMatch(role -> "company_admin".equals(role.getName()));
	}

	public static OrderOverviewDto toOverviewDto(OrderRecord orderRecord) {
		return new OrderOverviewDto(orderRecord);
	}

	public static OrderTrackingDTO toOrderTrackingDTO(OrderRecord orderRecord) {
        return new OrderTrackingDTO(orderRecord);
	}
}

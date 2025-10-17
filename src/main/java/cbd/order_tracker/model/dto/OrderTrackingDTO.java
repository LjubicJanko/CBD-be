package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.ContactInfo;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.OrderStatusHistory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderTrackingDTO {

	private String name;
	private String description;
	private LocalDate plannedEndingDate;
	private String lastUpdatedDate;
	private OrderStatus status;
	private boolean isLegalEntity;
	private BigDecimal amountLeftToPay;
	private BigDecimal amountLeftToPayWithTax;
	private String postalCode;
	private String postalService;
	private boolean extension;
	private ContactInfo contactInfo;

	public OrderTrackingDTO() {
	}

	public OrderTrackingDTO(OrderRecord orderRecord) {
		this.name = orderRecord.getName();
		this.description = orderRecord.getDescription();
		this.plannedEndingDate = orderRecord.getPlannedEndingDate();
		List<OrderStatusHistory> statusHistory = orderRecord.getStatusHistory();
		OrderStatusHistory lastItem = statusHistory.get(statusHistory.size() - 1);
		this.lastUpdatedDate = lastItem.getCreationTime().toString();
		this.status = orderRecord.getStatus();
		this.isLegalEntity = orderRecord.isLegalEntity();
		this.amountLeftToPay = orderRecord.getSalePrice().subtract(orderRecord.getAmountPaid());
		this.amountLeftToPayWithTax = orderRecord.getSalePriceWithTax().subtract(orderRecord.getAmountPaid());
		this.extension = orderRecord.getExtension();
		this.contactInfo = orderRecord.getContactInfo();
		if (orderRecord.getStatus() == OrderStatus.SHIPPED) {
			var history = orderRecord.getStatusHistory();
			var lastStatusHistoryChange = history.get(history.size() - 1);
			this.postalCode = lastStatusHistoryChange.getPostalCode();
			this.postalService = lastStatusHistoryChange.getPostalService();
		}
	}

}

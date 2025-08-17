package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OrderOverviewDto {

	private Long id;
	private String name;
	private String description;
	private LocalDate plannedEndingDate;
	private String dateWhenMovedToDone;
	private OrderStatus status;
	private OrderPriority priority;
	private OrderExecutionStatus executionStatus;
	private BigDecimal amountLeftToPay;
	private String postalCode;
	private String postalService;

	public OrderOverviewDto(OrderRecord orderRecord) {
		this.id = orderRecord.getId();
		this.name = orderRecord.getName();
		this.description = orderRecord.getDescription();
		this.plannedEndingDate = orderRecord.getPlannedEndingDate();

		this.status = orderRecord.getStatus();
		this.priority = orderRecord.getPriority();
		this.executionStatus = orderRecord.getExecutionStatus();

		this.dateWhenMovedToDone = String.valueOf(orderRecord.getDateWhenMovedToDone());
		this.postalCode = orderRecord.getPostalCode();
		this.postalService = orderRecord.getPostalService();

		BigDecimal priceForCalculation = orderRecord.isLegalEntity() ? orderRecord.getSalePriceWithTax() : orderRecord.getSalePrice();
		BigDecimal amountLefToPay = priceForCalculation.subtract(orderRecord.getAmountPaid());
		this.setAmountLeftToPay(amountLefToPay);
	}

	public OrderOverviewDto(
			Long id,
			String name,
			String description,
			LocalDate plannedEndingDate,
			OrderStatus status,
			OrderPriority priority,
			OrderExecutionStatus executionStatus,
			LocalDateTime dateWhenMovedToDone,
			String postalCode,
			String postalService,
			BigDecimal salePrice,
			BigDecimal salePriceWithTax,
			boolean legalEntity,
			BigDecimal amountPaid
	) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.plannedEndingDate = plannedEndingDate;
		this.status = status;
		this.priority = priority;
		this.executionStatus = executionStatus;
		this.dateWhenMovedToDone = dateWhenMovedToDone != null ? String.valueOf(dateWhenMovedToDone) : null;
		this.postalCode = postalCode;
		this.postalService = postalService;

		BigDecimal priceForCalculation = legalEntity ? salePriceWithTax : salePrice;
		BigDecimal amountLeftToPay = priceForCalculation.subtract(amountPaid);
		this.setAmountLeftToPay(amountLeftToPay);
	}


}

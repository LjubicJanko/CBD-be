package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderDTO {

	private Long id;
	private String name;
	private String description;
	private String note;
	private LocalDate plannedEndingDate;
	private String pausingComment;
	private OrderStatus status;
	private OrderPriority priority;
	private OrderExecutionStatus executionStatus;
	private String trackingId;
	private List<OrderStatusHistoryDTO> statusHistory;

	private List<Payment> payments;

	private boolean isLegalEntity;

	// Use BigDecimal for monetary values
	private BigDecimal acquisitionCost;
	private BigDecimal salePrice;
	private BigDecimal salePriceWithTax;
	private BigDecimal priceDifference;
	private BigDecimal amountPaid;
	private BigDecimal amountLeftToPay; // this should be calculated based on info if it is legal entity or not

	private String postalCode;
	private String postalService;
}

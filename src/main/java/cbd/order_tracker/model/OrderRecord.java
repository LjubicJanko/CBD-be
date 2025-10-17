package cbd.order_tracker.model;

import cbd.order_tracker.model.dto.PaymentRequestDto;
import cbd.order_tracker.model.dto.request.OrderExtensionReqDto;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@SQLRestriction("deleted = false")
public class OrderRecord {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String description;
	private String note;
	@Column(name = "planned_ending_date", columnDefinition = "DATE")
	private LocalDate plannedEndingDate;
	private String trackingId;
	private String pausingComment;
	private LocalDateTime creationTime;
	private boolean deleted = false;

	@Column(name = "legal_entity", nullable = false)
	private boolean legalEntity;

	@Column(precision = 19, scale = 4)
	private BigDecimal acquisitionCost;

	@Column(precision = 19, scale = 4)
	private BigDecimal salePrice;

	@Column(precision = 19, scale = 4)
	private BigDecimal salePriceWithTax;

	@Column(precision = 19, scale = 4)
	private BigDecimal priceDifference;

	@Column(precision = 19, scale = 4)
	private BigDecimal amountPaid;

	@Column(precision = 19, scale = 4)
	private BigDecimal amountLeftToPay;

	@Column(precision = 19, scale = 4)
	private BigDecimal amountLeftToPayWithTax;

	private LocalDateTime dateWhenMovedToDone;
	private String postalCode;
	private String postalService;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private OrderStatus status;

	@Column(length = 32, columnDefinition = "varchar(32) default 'MEDIUM'")
	@Enumerated(EnumType.STRING)
	private OrderPriority priority;

	@Enumerated(EnumType.STRING)
	private OrderExecutionStatus executionStatus;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<OrderStatusHistory> statusHistory;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Payment> payments;

	private ContactInfo contactInfo;

	@Column(name = "extension", nullable = false)
	private Boolean extension = false;

	public OrderRecord() {
	}

	public OrderRecord(OrderExtensionReqDto extensionReqDto) {
		this.name = extensionReqDto.getName();
		this.description = extensionReqDto.getDescription();
		this.contactInfo = extensionReqDto.getContactInfo();
		this.trackingId = UUID.randomUUID().toString().substring(0, 8);
		this.creationTime = LocalDateTime.now();

//		default values
		this.executionStatus = OrderExecutionStatus.ACTIVE;
		this.status = OrderStatus.PENDING;
		this.acquisitionCost = BigDecimal.ZERO;
		this.statusHistory = new ArrayList<>();
		this.payments = new ArrayList<>();
		this.extension = true;
		this.amountPaid = BigDecimal.ZERO;
		this.salePrice = BigDecimal.ZERO;
		this.salePriceWithTax = BigDecimal.ZERO;
		this.amountLeftToPay = BigDecimal.ZERO;
		this.amountLeftToPayWithTax = BigDecimal.ZERO;
		this.priority = OrderPriority.MEDIUM;
		this.dateWhenMovedToDone = null;
		this.postalCode = null;
		this.postalService = null;

		addStatusHistory(status, null, null);
	}

	//	todo: check this copy constructor
	public OrderRecord(OrderRecord order) {
		this.name = order.getName();
		this.description = order.getDescription();
		this.note = order.getNote();
		this.plannedEndingDate = order.getPlannedEndingDate();
		this.trackingId = UUID.randomUUID().toString().substring(0, 8);
		this.legalEntity = order.isLegalEntity();
		this.acquisitionCost = order.getAcquisitionCost();
		this.salePrice = order.getSalePrice();
		this.salePriceWithTax = order.getSalePrice().multiply(BigDecimal.valueOf(1.2));
		this.priceDifference = order.getSalePrice().subtract(acquisitionCost);
		this.amountPaid = BigDecimal.ZERO;
		this.amountLeftToPay = order.getSalePrice();
		this.amountLeftToPayWithTax = salePriceWithTax;
		this.creationTime = LocalDateTime.now();
		this.status = OrderStatus.DESIGN;
		this.priority = order.getPriority();
		this.executionStatus = OrderExecutionStatus.ACTIVE;
		this.statusHistory = new ArrayList<>();
		this.payments = new ArrayList<>();
		this.dateWhenMovedToDone = null;
		this.postalCode = null;
		this.postalService = null;
		addStatusHistory(status, null, null);
	}


	public void nextStatus(String postalCode, String postalService) {
		this.status = this.status.next();
		if(status.equals(OrderStatus.SHIPPED)) {
			this.postalService = postalService;
			this.postalCode = postalCode;
		} else if(status.equals(OrderStatus.DONE)) {
			this.dateWhenMovedToDone = LocalDateTime.now();
		}
		addStatusHistory(status, postalCode, postalService);
	}

	private void addStatusHistory(OrderStatus newStatus, String postalCode, String postalService) {
		statusHistory.add(new OrderStatusHistory(this, newStatus, postalCode, postalService));
	}

	public void addPayment(PaymentRequestDto payment) {
		Payment newPayment = new Payment(this, payment);
		this.payments.add(newPayment);
		this.amountPaid = this.amountPaid.add(newPayment.getAmount());
		this.amountLeftToPay = this.salePrice.subtract(this.amountPaid);
		this.amountLeftToPayWithTax = this.salePriceWithTax.subtract(this.amountPaid);
	}


}

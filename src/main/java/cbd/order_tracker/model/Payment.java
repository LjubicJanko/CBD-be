package cbd.order_tracker.model;

import cbd.order_tracker.model.dto.PaymentRequestDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "order_id", nullable = false)
	@JsonIgnore
	private OrderRecord order;

	private String payer; // For individual: name and surname, for legal entity: company name

	@Column(precision = 19, scale = 2, nullable = false)
	private BigDecimal amount; // Amount in RSD

	@Column(nullable = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
	private LocalDate paymentDate; // Payment date, defaults to today

	@Enumerated(EnumType.STRING)
	private PaymentMethod paymentMethod; // Payment method options: Account, Cash, Invoice

	private String note; // Optional field for additional notes

	public Payment() {
	}

	public Payment(OrderRecord order, PaymentRequestDto payment) {
		this.order = order;
		this.payer = payment.getPayer();
		this.amount = payment.getAmount();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate localDate = LocalDate.parse(payment.getPaymentDate(), formatter);
		this.paymentDate = localDate;
		this.paymentMethod = payment.getPaymentMethod();
		this.note = payment.getNote();
	}

	public Long getId() {
		return id;
	}

	public OrderRecord getOrder() {
		return order;
	}

	public void setOrder(OrderRecord order) {
		this.order = order;
	}

	public String getPayer() {
		return payer;
	}

	public void setPayer(String payer) {
		this.payer = payer;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public LocalDate getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(LocalDate paymentDate) {
		this.paymentDate = paymentDate;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}

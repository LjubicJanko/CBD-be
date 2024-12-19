package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentRequestDto {

	private OrderRecord order;

	private String payer; // For individual: name and surname, for legal entity: company name
	private BigDecimal amount; // Amount in RSD
	private String paymentDate; // Payment date, defaults to today
	private PaymentMethod paymentMethod; // Payment method options: Account, Cash, Invoice, CASH_ON_DELIVERY
	private String note;

	public PaymentRequestDto() {
	}

	public PaymentRequestDto(OrderRecord order, String payer, BigDecimal amount, String paymentDate, PaymentMethod paymentMethod, String note) {
		this.order = order;
		this.payer = payer;
		this.amount = amount;
		this.paymentDate = paymentDate;
		this.paymentMethod = paymentMethod;
		this.note = note;
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

	public String getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(String paymentDate) {
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

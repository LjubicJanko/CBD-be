package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.Payment;
import cbd.order_tracker.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@AllArgsConstructor
@Getter
@Setter
public class PaymentDto {
    private Long id;
    private String payer;
    private BigDecimal amount;
    private String paymentDate;
    private PaymentMethod paymentMethod;
    private String note;

    public PaymentDto(Payment payment) {
        this.id = payment.getId();
        this.payer = payment.getPayer();
        this.amount = payment.getAmount();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        this.paymentDate = payment.getPaymentDate().format(formatter);

        this.paymentMethod = payment.getPaymentMethod();
        this.note = payment.getNote();
    }
}

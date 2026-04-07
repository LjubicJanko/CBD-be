package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.Payment;
import cbd.order_tracker.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
public class PaymentDto {
    private Long id;
    private String payer;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private String note;

    public PaymentDto(Payment payment) {
        this.id = payment.getId();
        this.payer = payment.getPayer();
        this.amount = payment.getAmount();
        this.paymentDate = payment.getPaymentDate();
        this.paymentMethod = payment.getPaymentMethod();
        this.note = payment.getNote();
    }
}

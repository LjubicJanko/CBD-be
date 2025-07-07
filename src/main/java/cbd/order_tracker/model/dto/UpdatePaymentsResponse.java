package cbd.order_tracker.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdatePaymentsResponse {
    private List<PaymentDto> payments;
    private BigDecimal amountPaid;
    private BigDecimal amountLeftToPay;
}

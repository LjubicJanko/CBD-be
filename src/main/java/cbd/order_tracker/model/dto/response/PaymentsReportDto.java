package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.dto.PageableResponse;

import java.util.List;

public class PaymentsReportDto extends PageableResponse<PaymentReportRowDto> {
    public String currency;
    public PaymentsTotalsDto totals;

    public PaymentsReportDto() {
    }

    public PaymentsReportDto(Integer page, Integer perPage, Integer total, Long totalElements,
                             List<PaymentReportRowDto> data, String currency, PaymentsTotalsDto totals) {
        super(page, perPage, total, totalElements, data);
        this.currency = currency;
        this.totals = totals;
    }
}

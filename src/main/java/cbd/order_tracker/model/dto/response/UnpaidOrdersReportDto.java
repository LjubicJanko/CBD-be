package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.dto.PageableResponse;

import java.util.List;

public class UnpaidOrdersReportDto extends PageableResponse<UnpaidOrderRowDto> {
    public String currency;
    public UnpaidTotalsDto totals;

    public UnpaidOrdersReportDto() {
    }

    public UnpaidOrdersReportDto(Integer page, Integer perPage, Integer total, Long totalElements,
                                 List<UnpaidOrderRowDto> data, String currency, UnpaidTotalsDto totals) {
        super(page, perPage, total, totalElements, data);
        this.currency = currency;
        this.totals = totals;
    }
}

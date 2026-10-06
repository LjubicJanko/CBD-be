package cbd.order_tracker.service;

import cbd.order_tracker.model.dto.response.OrderReportDto;
import cbd.order_tracker.model.dto.response.PaymentsReportDto;
import cbd.order_tracker.model.dto.response.StatusDurationReportDto;
import cbd.order_tracker.model.dto.response.UnpaidOrdersReportDto;

import java.time.LocalDate;

public interface ReportService {
    OrderReportDto getOrderReport(LocalDate from, LocalDate to);
    StatusDurationReportDto getStatusDurationReport(LocalDate from, LocalDate to);
    PaymentsReportDto getPaymentsReport(LocalDate from, LocalDate to, String methods, Integer page, Integer perPage, String sort);
    UnpaidOrdersReportDto getUnpaidOrders(LocalDate from, LocalDate to, Boolean withoutPaymentsInRange, Integer page, Integer perPage, String sort);
}

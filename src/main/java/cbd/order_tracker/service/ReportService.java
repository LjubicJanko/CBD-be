package cbd.order_tracker.service;

import cbd.order_tracker.model.dto.response.OrderReportDto;
import cbd.order_tracker.model.dto.response.StatusDurationReportDto;

import java.time.LocalDate;

public interface ReportService {
    OrderReportDto getOrderReport(LocalDate from, LocalDate to);
    StatusDurationReportDto getStatusDurationReport(LocalDate from, LocalDate to);
}

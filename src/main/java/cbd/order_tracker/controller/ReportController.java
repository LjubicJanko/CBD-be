package cbd.order_tracker.controller;

import cbd.order_tracker.model.dto.response.OrderReportDto;
import cbd.order_tracker.model.dto.response.PaymentsReportDto;
import cbd.order_tracker.model.dto.response.StatusDurationReportDto;
import cbd.order_tracker.model.dto.response.UnpaidOrdersReportDto;
import cbd.order_tracker.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/orders")
    public ResponseEntity<OrderReportDto> getOrderReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getOrderReport(from, to));
    }

    @GetMapping("/status-duration")
    public ResponseEntity<StatusDurationReportDto> getStatusDurationReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getStatusDurationReport(from, to));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('company_admin','SUPERADMIN')")
    public ResponseEntity<PaymentsReportDto> getPaymentsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String methods,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer perPage,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(reportService.getPaymentsReport(from, to, methods, page, perPage, sort));
    }

    @GetMapping("/unpaid-orders")
    @PreAuthorize("hasAnyRole('company_admin','SUPERADMIN')")
    public ResponseEntity<UnpaidOrdersReportDto> getUnpaidOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Boolean withoutPaymentsInRange,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer perPage,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(reportService.getUnpaidOrders(from, to, withoutPaymentsInRange, page, perPage, sort));
    }
}

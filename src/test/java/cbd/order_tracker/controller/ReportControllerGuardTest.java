package cbd.order_tracker.controller;

import cbd.order_tracker.model.dto.response.PaymentsReportDto;
import cbd.order_tracker.model.dto.response.UnpaidOrdersReportDto;
import cbd.order_tracker.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * These tests verify the {@code @PreAuthorize} annotation by reflection only. They do NOT verify the Spring
 * Security interception itself (not evaluated in plain unit tests), so the actual 403/200 responses need a
 * running app or a security slice.
 */
class ReportControllerGuardTest {

    private static final String EXPECTED = "hasAnyRole('company_admin','SUPERADMIN')";

    private static PreAuthorize paymentsGuard() throws NoSuchMethodException {
        Method m = ReportController.class.getMethod("getPaymentsReport", LocalDate.class, LocalDate.class,
                String.class, Integer.class, Integer.class, String.class);
        return m.getAnnotation(PreAuthorize.class);
    }

    private static PreAuthorize unpaidGuard() throws NoSuchMethodException {
        Method m = ReportController.class.getMethod("getUnpaidOrders", LocalDate.class, LocalDate.class,
                Boolean.class, Integer.class, Integer.class, String.class);
        return m.getAnnotation(PreAuthorize.class);
    }

    @Test
    void paymentsReportIsRestrictedToCompanyAdminAndSuperadmin() throws NoSuchMethodException {
        assertThat(paymentsGuard()).isNotNull();
        assertThat(paymentsGuard().value()).isEqualTo(EXPECTED);
    }

    @Test
    void nonAdminRolesAreRejectedOnThePaymentsReport() throws NoSuchMethodException {
        assertThat(paymentsGuard().value()).doesNotContain("manager").doesNotContain("manufacturer");
    }

    @Test
    void superadminIsAllowedOnThePaymentsReport() throws NoSuchMethodException {
        assertThat(paymentsGuard().value()).contains("'SUPERADMIN'");
    }

    @Test
    void unpaidOrdersIsRestrictedToCompanyAdminAndSuperadmin() throws NoSuchMethodException {
        assertThat(unpaidGuard()).isNotNull();
        assertThat(unpaidGuard().value()).isEqualTo(EXPECTED);
    }

    @Test
    void nonAdminRolesAreRejectedOnUnpaidOrders() throws NoSuchMethodException {
        assertThat(unpaidGuard().value()).doesNotContain("manager").doesNotContain("manufacturer");
    }

    // --- controller pass-through (mocked service) ---

    private final ReportService service = mock(ReportService.class);
    private final ReportController controller = new ReportController(service);

    @Test
    void getPaymentsReportReturns200WithTheServiceBodyAndPassesEachParameterInPosition() {
        PaymentsReportDto body = new PaymentsReportDto();
        LocalDate from = LocalDate.parse("2026-01-02");
        LocalDate to = LocalDate.parse("2026-03-04");
        when(service.getPaymentsReport(from, to, "CASH,ACCOUNT", 3, 25, "asc")).thenReturn(body);

        ResponseEntity<PaymentsReportDto> response =
                controller.getPaymentsReport(from, to, "CASH,ACCOUNT", 3, 25, "asc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
        verify(service).getPaymentsReport(from, to, "CASH,ACCOUNT", 3, 25, "asc");
    }

    @Test
    void getUnpaidOrdersReturns200WithTheServiceBodyAndPassesEachParameterInPosition() {
        UnpaidOrdersReportDto body = new UnpaidOrdersReportDto();
        LocalDate from = LocalDate.parse("2026-01-02");
        LocalDate to = LocalDate.parse("2026-03-04");
        when(service.getUnpaidOrders(from, to, true, 3, 25, "asc")).thenReturn(body);

        ResponseEntity<UnpaidOrdersReportDto> response =
                controller.getUnpaidOrders(from, to, true, 3, 25, "asc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
        verify(service).getUnpaidOrders(from, to, true, 3, 25, "asc");
    }
}

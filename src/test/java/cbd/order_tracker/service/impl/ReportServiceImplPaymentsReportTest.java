package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.PaymentMethod;
import cbd.order_tracker.model.dto.response.PaymentReportRowDto;
import cbd.order_tracker.model.dto.response.PaymentsReportDto;
import cbd.order_tracker.repository.OrderRepository;
import cbd.order_tracker.repository.OrderStatusHistoryRepository;
import cbd.order_tracker.repository.PaymentRepository;
import cbd.order_tracker.util.UserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Payments report scenarios. The repository is mocked: DB behavior (tenant and soft-delete filtering, JPQL
 * typing, date and method filtering in SQL) is NOT exercised here; only service logic and the query text.
 */
class ReportServiceImplPaymentsReportTest {

    private static final Long TENANT_ID = 1L;

    private PaymentRepository paymentRepository;
    private ReportServiceImpl service;

    @BeforeEach
    void setup() {
        paymentRepository = mock(PaymentRepository.class);
        service = new ReportServiceImpl(mock(OrderRepository.class), mock(OrderStatusHistoryRepository.class),
                paymentRepository, mock(UserUtil.class));
        TenantContext.setSuperadmin(false);
        TenantContext.setTenantId(TENANT_ID);
        stubRows(List.of(), 0);
        stubGroups();
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    // ---- helpers ----

    private void stubRows(List<PaymentReportRowDto> rows, long totalElements) {
        when(paymentRepository.findPaymentReportRows(any(), any(), any(), anyBoolean(), anyBoolean(), any(),
                anyBoolean(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(rows, inv.getArgument(7, Pageable.class), totalElements));
    }

    private void stubGroups(Object[]... groups) {
        when(paymentRepository.sumPaymentsByMethod(any(), any(), any())).thenReturn(new ArrayList<>(List.of(groups)));
    }

    private static Object[] group(PaymentMethod method, String sum, long count) {
        return new Object[]{method, new BigDecimal(sum), count};
    }

    private static PaymentReportRowDto row(long id, String amount, String date, PaymentMethod method) {
        return new PaymentReportRowDto(id, 41L, "Hoodies", "a1b2c3d4", "Marko M", new BigDecimal(amount),
                LocalDate.parse(date), method, null);
    }

    private PaymentsReportDto report() {
        return service.getPaymentsReport(null, null, null, null, null, null);
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentRepository).findPaymentReportRows(any(), any(), any(), anyBoolean(), anyBoolean(), any(),
                anyBoolean(), captor.capture());
        return captor.getValue();
    }

    private static String listQuery() throws NoSuchMethodException {
        Method m = PaymentRepository.class.getMethod("findPaymentReportRows", Long.class, LocalDate.class,
                LocalDate.class, boolean.class, boolean.class, List.class, boolean.class, Pageable.class);
        return m.getAnnotation(Query.class).value();
    }

    private static String groupQuery() throws NoSuchMethodException {
        Method m = PaymentRepository.class.getMethod("sumPaymentsByMethod", Long.class, LocalDate.class, LocalDate.class);
        return m.getAnnotation(Query.class).value();
    }

    private static void assertMoney(BigDecimal actual, String expected) {
        assertThat(actual).isEqualTo(new BigDecimal(expected));
        assertThat(actual.scale()).isEqualTo(2);
    }

    // ---- filtering, totals ----

    @Test
    void dateRangeIsInclusiveOnBothEnds() {
        stubRows(List.of(row(1, "100.00", "2026-09-01", PaymentMethod.CASH),
                row(2, "100.00", "2026-09-15", PaymentMethod.CASH),
                row(3, "100.00", "2026-09-30", PaymentMethod.CASH)), 3);
        stubGroups(group(PaymentMethod.CASH, "300.00", 3));

        PaymentsReportDto result = service.getPaymentsReport(LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-30"), null, null, null, null);

        assertThat(result.data).hasSize(3);
        assertMoney(result.totals.overall(), "300.00");
        assertThat(result.totals.count()).isEqualTo(3);
        verify(paymentRepository).findPaymentReportRows(eq(TENANT_ID), eq(LocalDate.parse("2026-09-01")),
                eq(LocalDate.parse("2026-09-30")), anyBoolean(), anyBoolean(), any(), anyBoolean(), any(Pageable.class));
        assertThat(PaymentRepository.REPORT_BASE_WHERE)
                .contains("p.paymentDate >= :from").contains("p.paymentDate <= :to");
    }

    @Test
    void bothBoundsOmittedReturnsAllHistory() {
        stubRows(List.of(row(1, "10.00", "2025-05-01", null), row(2, "10.00", "2026-05-01", null)), 2);

        PaymentsReportDto result = report();

        assertThat(result.totalElements).isEqualTo(2);
        verify(paymentRepository).findPaymentReportRows(eq(TENANT_ID), eq(null), eq(null), anyBoolean(),
                anyBoolean(), any(), anyBoolean(), any(Pageable.class));
        verify(paymentRepository).sumPaymentsByMethod(TENANT_ID, null, null);
    }

    @Test
    void fromAfterToIsRejected() {
        assertThatThrownBy(() -> service.getPaymentsReport(LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-09-01"), null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(paymentRepository, never()).findPaymentReportRows(any(), any(), any(), anyBoolean(), anyBoolean(),
                any(), anyBoolean(), any(Pageable.class));
    }

    @Test
    void methodsFilterNarrowsDataOverallAndCount() {
        stubRows(List.of(row(1, "100.00", "2026-09-10", PaymentMethod.CASH)), 1);
        stubGroups(group(PaymentMethod.CASH, "100.00", 1), group(PaymentMethod.ACCOUNT, "200.00", 1));

        PaymentsReportDto result = service.getPaymentsReport(null, null, "CASH", null, null, null);

        assertThat(result.data).hasSize(1);
        assertThat(result.data.get(0).paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertMoney(result.totals.overall(), "100.00");
        assertThat(result.totals.count()).isEqualTo(1);
        assertThat(result.totalElements).isEqualTo(1);
        verify(paymentRepository).findPaymentReportRows(eq(TENANT_ID), any(), any(), eq(true), eq(true),
                eq(List.of(PaymentMethod.CASH)), eq(false), any(Pageable.class));
    }

    @Test
    void byMethodAndBankTotalIgnoreTheMethodsFilter() {
        stubGroups(group(PaymentMethod.CASH, "100.00", 1), group(PaymentMethod.ACCOUNT, "200.00", 1));

        PaymentsReportDto result = service.getPaymentsReport(null, null, "CASH", null, null, null);

        assertMoney(result.totals.byMethod().get("ACCOUNT"), "200.00");
        assertMoney(result.totals.byMethod().get("CASH"), "100.00");
        assertMoney(result.totals.bankTotal(), "200.00");
    }

    @Test
    void bankTotalIsAccountPlusInvoicePlusOnShip() {
        stubGroups(group(PaymentMethod.ACCOUNT, "100.00", 1), group(PaymentMethod.INVOICE, "50.00", 1),
                group(PaymentMethod.ON_SHIP, "25.00", 1), group(PaymentMethod.CASH, "10.00", 1),
                group(null, "5.00", 1));

        PaymentsReportDto result = report();

        assertMoney(result.totals.bankTotal(), "175.00");
        assertMoney(result.totals.overall(), "190.00");
    }

    @Test
    void nullPaymentMethodIsBucketedAsUnspecifiedAndNeverInBankTotal() {
        stubRows(List.of(row(1, "40.00", "2026-09-10", null)), 1);
        stubGroups(group(null, "40.00", 1));

        PaymentsReportDto result = report();

        assertMoney(result.totals.byMethod().get("UNSPECIFIED"), "40.00");
        assertMoney(result.totals.bankTotal(), "0.00");
        assertThat(result.data.get(0).paymentMethod()).isNull();
    }

    @Test
    void methodsUnspecifiedSelectsTheNullMethodPayments() {
        stubRows(List.of(row(2, "5.00", "2026-09-10", null)), 1);
        stubGroups(group(PaymentMethod.CASH, "10.00", 1), group(null, "5.00", 1));

        PaymentsReportDto result = service.getPaymentsReport(null, null, "UNSPECIFIED", null, null, null);

        assertThat(result.data).hasSize(1);
        assertThat(result.data.get(0).paymentMethod()).isNull();
        assertMoney(result.totals.overall(), "5.00");
        verify(paymentRepository).findPaymentReportRows(eq(TENANT_ID), any(), any(), eq(true), eq(false),
                any(), eq(true), any(Pageable.class));
        assertThat(PaymentRepository.METHODS_FILTER).contains("p.paymentMethod IS NULL");
    }

    @Test
    void unknownMethodsTokenIsRejected() {
        assertThatThrownBy(() -> service.getPaymentsReport(null, null, "BITCOIN", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyResultStillReturnsEveryByMethodKey() {
        PaymentsReportDto result = report();

        assertThat(result.data).isEmpty();
        assertThat(result.total).isZero();
        assertThat(result.totalElements).isZero();
        assertMoney(result.totals.overall(), "0.00");
        assertThat(result.totals.count()).isZero();
        assertThat(result.totals.byMethod().keySet())
                .containsExactly("ACCOUNT", "CASH", "ON_SHIP", "INVOICE", "UNSPECIFIED");
        result.totals.byMethod().values().forEach(v -> assertMoney(v, "0.00"));
        assertThat(result.currency).isEqualTo("RSD");
    }

    @Test
    void totalsCoverTheWholeFilteredSetNotThePage() {
        List<PaymentReportRowDto> pageRows = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            pageRows.add(row(i, "10.00", "2026-09-10", PaymentMethod.CASH));
        }
        stubRows(pageRows, 120);
        stubGroups(group(PaymentMethod.CASH, "1200.00", 120));

        PaymentsReportDto result = service.getPaymentsReport(null, null, null, 1, 50, null);

        assertThat(result.data).hasSize(50);
        assertThat(result.totalElements).isEqualTo(120);
        assertThat(result.total).isEqualTo(3);
        assertMoney(result.totals.overall(), "1200.00");
        assertThat(capturedPageable().getPageNumber()).isEqualTo(1);
    }

    @Test
    void defaultSortIsPaymentDateDescendingWithIdTieBreak() {
        report();

        Sort sort = capturedPageable().getSort();
        assertThat(sort.getOrderFor("paymentDate").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.stream().map(Sort.Order::getProperty)).containsExactly("paymentDate", "id");
    }

    @Test
    void ascendingSortReversesTheOrderIncludingTheIdTieBreak() {
        service.getPaymentsReport(null, null, null, null, null, "ASC");

        Sort sort = capturedPageable().getSort();
        assertThat(sort.getOrderFor("paymentDate").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void itemShape() {
        PaymentReportRowDto item = new PaymentReportRowDto(88L, 41L, "Hoodies", "a1b2c3d4", "Marko M",
                new BigDecimal("12000.00"), LocalDate.parse("2026-09-30"), PaymentMethod.CASH, "n");
        stubRows(List.of(item), 1);

        PaymentReportRowDto result = report().data.get(0);

        assertThat(result.id()).isEqualTo(88L);
        assertThat(result.orderId()).isEqualTo(41L);
        assertThat(result.orderName()).isEqualTo("Hoodies");
        assertThat(result.trackingId()).isEqualTo("a1b2c3d4");
        assertThat(result.payer()).isEqualTo("Marko M");
        assertThat(result.amount()).isEqualByComparingTo("12000.00");
        assertThat(result.paymentDate()).isEqualTo(LocalDate.parse("2026-09-30"));
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(result.note()).isEqualTo("n");
    }

    @Test
    void totalsAreRoundedHalfUpToTwoDecimalsOnceAtTheEnd() {
        stubGroups(group(PaymentMethod.ACCOUNT, "100.005", 1));

        PaymentsReportDto result = report();

        assertMoney(result.totals.byMethod().get("ACCOUNT"), "100.01");
    }

    // ---- tenant and soft-delete ----

    @Test
    void otherTenantsPaymentsNeverAppear() {
        report();

        // scoping is done in the query: the service passes the caller tenant (1) and the query filters on it
        verify(paymentRepository).findPaymentReportRows(eq(TENANT_ID), any(), any(), anyBoolean(), anyBoolean(),
                any(), anyBoolean(), any(Pageable.class));
        verify(paymentRepository).sumPaymentsByMethod(eq(TENANT_ID), any(), any());
        assertThat(PaymentRepository.REPORT_BASE_WHERE).contains("o.tenant.id = :tenantId");
    }

    @Test
    void paymentsOfSoftDeletedOrdersAreExcluded() throws NoSuchMethodException {
        for (String q : List.of(listQuery(), groupQuery())) {
            assertThat(q).contains("JOIN p.order o").contains("o.deleted = false").contains("o.tenant.id = :tenantId");
        }
    }

    @Test
    void paymentsOfCanceledOrdersAreIncluded() throws NoSuchMethodException {
        assertThat(listQuery()).doesNotContain("executionStatus");
        assertThat(groupQuery()).doesNotContain("executionStatus");
    }

    @Test
    void paymentRowsAreLoadedAsProjectionsNotEntities() throws NoSuchMethodException {
        Method m = PaymentRepository.class.getMethod("findPaymentReportRows", Long.class, LocalDate.class,
                LocalDate.class, boolean.class, boolean.class, List.class, boolean.class, Pageable.class);

        assertThat(listQuery()).startsWith("SELECT new cbd.order_tracker.model.dto.response.PaymentReportRowDto(");
        assertThat(m.getGenericReturnType().getTypeName()).contains("PaymentReportRowDto");
    }

    // ---- pagination validation ----

    @Test
    void perPageDefaultsTo50AndPageTo0OnPayments() {
        PaymentsReportDto result = report();

        assertThat(result.page).isZero();
        assertThat(result.perPage).isEqualTo(50);
    }

    @ParameterizedTest
    @CsvSource({"-1,50", "0,0", "0,101"})
    void invalidPaginationIsRejectedOnPayments(int page, int perPage) {
        assertThatThrownBy(() -> service.getPaymentsReport(null, null, null, page, perPage, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void perPage1IsAcceptedOnPayments() {
        PaymentsReportDto result = service.getPaymentsReport(null, null, null, null, 1, null);

        assertThat(result.perPage).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void methodsUnspecifiedPassesANonEmptyMethodsListToTheRepository() {
        service.getPaymentsReport(null, null, "UNSPECIFIED", null, null, null);

        ArgumentCaptor<List<PaymentMethod>> captor = ArgumentCaptor.forClass(List.class);
        verify(paymentRepository).findPaymentReportRows(any(), any(), any(), eq(true), eq(false),
                captor.capture(), eq(true), any(Pageable.class));
        assertThat(captor.getValue()).isNotEmpty();
    }

    @Test
    void methodsAccountAndInvoicePassesBothEnumsInOrder() {
        service.getPaymentsReport(null, null, "ACCOUNT,INVOICE", null, null, null);

        verify(paymentRepository).findPaymentReportRows(any(), any(), any(), eq(true), eq(true),
                eq(List.of(PaymentMethod.ACCOUNT, PaymentMethod.INVOICE)), eq(false), any(Pageable.class));
    }

    @Test
    void perPage100IsAccepted() {
        PaymentsReportDto result = service.getPaymentsReport(null, null, null, null, 100, null);

        assertThat(result.perPage).isEqualTo(100);
    }
}

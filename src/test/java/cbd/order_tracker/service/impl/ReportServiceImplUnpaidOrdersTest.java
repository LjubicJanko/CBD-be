package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.dto.response.UnpaidOrderRowDto;
import cbd.order_tracker.model.dto.response.UnpaidOrdersReportDto;
import cbd.order_tracker.repository.OrderRepository;
import cbd.order_tracker.repository.OrderStatusHistoryRepository;
import cbd.order_tracker.repository.PaymentRepository;
import cbd.order_tracker.util.UserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
 * Unpaid orders scenarios. The repository is mocked: balance computation, filtering, sorting and the NOT EXISTS
 * and correlated subqueries run in the database and are NOT exercised here; only service logic and query text.
 */
class ReportServiceImplUnpaidOrdersTest {

    private static final Long TENANT_ID = 1L;

    private OrderRepository orderRepository;
    private ReportServiceImpl service;

    @BeforeEach
    void setup() {
        orderRepository = mock(OrderRepository.class);
        service = new ReportServiceImpl(orderRepository, mock(OrderStatusHistoryRepository.class),
                mock(PaymentRepository.class), mock(UserUtil.class));
        TenantContext.setSuperadmin(false);
        TenantContext.setTenantId(TENANT_ID);
        stub(List.of(), 0, null);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    // ---- helpers ----

    private void stub(List<UnpaidOrderRowDto> rows, long totalElements, String outstanding) {
        when(orderRepository.findUnpaidOrdersDesc(any(), any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(rows, inv.getArgument(6, Pageable.class), totalElements));
        when(orderRepository.findUnpaidOrdersAsc(any(), any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(rows, inv.getArgument(6, Pageable.class), totalElements));
        when(orderRepository.sumUnpaidBalance(any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(outstanding == null ? null : new BigDecimal(outstanding));
    }

    private static UnpaidOrderRowDto row(long id, String salePrice, String paid, String left) {
        return new UnpaidOrderRowDto(id, "Order " + id, "trk" + id, OrderStatus.PENDING, OrderExecutionStatus.ACTIVE,
                new BigDecimal(salePrice), new BigDecimal(paid), new BigDecimal(left), null, 0L);
    }

    private UnpaidOrdersReportDto unpaid() {
        return service.getUnpaidOrders(null, null, null, null, null, null);
    }

    private static String listDescQuery() throws NoSuchMethodException {
        return queryOf("findUnpaidOrdersDesc");
    }

    private static String queryOf(String name) throws NoSuchMethodException {
        for (Method m : OrderRepository.class.getMethods()) {
            if (m.getName().equals(name)) {
                return m.getAnnotation(Query.class).value();
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static void assertMoney(BigDecimal actual, String expected) {
        assertThat(actual).isEqualTo(new BigDecimal(expected));
        assertThat(actual.scale()).isEqualTo(2);
    }

    // ---- balance ----

    @Test
    void individualOrderBalanceUsesSalePrice() {
        stub(List.of(row(1, "1000.0000", "400.0000", "600.0000")), 1, "600.0000");

        UnpaidOrderRowDto result = unpaid().data.get(0);

        assertMoney(result.amountLeftToPay(), "600.00");
        assertThat(listBalanceExpression()).contains("ELSE COALESCE(o.salePrice, 0) END");
    }

    @Test
    void legalEntityBalanceUsesSalePriceWithTax() {
        // the repository computes 1200 - 400 = 800 for a legal entity; salePrice stays net
        stub(List.of(row(1, "1000.0000", "400.0000", "800.0000")), 1, "800.0000");

        UnpaidOrderRowDto result = unpaid().data.get(0);

        assertMoney(result.amountLeftToPay(), "800.00");
        assertMoney(result.salePrice(), "1000.00");
        assertThat(OrderRepository.UNPAID_BALANCE)
                .contains("CASE WHEN o.legalEntity = true THEN COALESCE(o.salePriceWithTax, 0)");
    }

    @Test
    void balanceIsComputedNotReadFromTheStoredColumn() throws NoSuchMethodException {
        stub(List.of(row(1, "500.0000", "100.0000", "400.0000")), 1, "400.0000");

        assertMoney(unpaid().data.get(0).amountLeftToPay(), "400.00");
        for (String name : List.of("findUnpaidOrdersDesc", "findUnpaidOrdersAsc", "sumUnpaidBalance", "getOutstandingTotal")) {
            assertThat(queryOf(name)).doesNotContain("amountLeftToPay");
        }
    }

    @Test
    void overpaidOrdersAreExcluded() throws NoSuchMethodException {
        assertThat(listDescQuery()).contains(OrderRepository.UNPAID_BALANCE + " > 0");
    }

    @Test
    void zeroBalanceOrdersAreExcluded() throws NoSuchMethodException {
        // strictly greater than zero: a balance of exactly 0 does not satisfy the predicate
        assertThat(listDescQuery()).contains(" > 0").doesNotContain(">= 0");
        assertThat(queryOf("sumUnpaidBalance")).contains(OrderRepository.UNPAID_BALANCE + " > 0");
    }

    @Test
    void canceledOrdersAreExcludedFromUnpaidOrders() throws NoSuchMethodException {
        assertThat(listDescQuery()).contains(
                "o.executionStatus <> cbd.order_tracker.model.OrderExecutionStatus.CANCELED");
    }

    @Test
    void archivedPausedAndActiveOrdersWithABalanceAreListed() throws NoSuchMethodException {
        // the only execution status predicate is <> CANCELED, so ARCHIVED, PAUSED and ACTIVE all pass
        String q = listDescQuery();
        assertThat(q).doesNotContain("ARCHIVED").doesNotContain("PAUSED").doesNotContain("ACTIVE");
        List<UnpaidOrderRowDto> rows = new ArrayList<>();
        for (OrderExecutionStatus status : List.of(OrderExecutionStatus.ARCHIVED, OrderExecutionStatus.PAUSED,
                OrderExecutionStatus.ACTIVE)) {
            rows.add(new UnpaidOrderRowDto((long) rows.size(), "o", "t", OrderStatus.PENDING, status,
                    new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("10"), null, 0L));
        }
        stub(rows, 3, "30");

        assertThat(unpaid().data).hasSize(3);
    }

    @Test
    void softDeletedOrdersAreExcludedFromUnpaidOrders() throws NoSuchMethodException {
        for (String name : List.of("findUnpaidOrdersDesc", "findUnpaidOrdersAsc", "sumUnpaidBalance")) {
            assertThat(queryOf(name)).contains("o.deleted = false");
        }
    }

    @Test
    void otherTenantsOrdersAreExcludedFromUnpaidOrders() throws NoSuchMethodException {
        unpaid();

        verify(orderRepository).findUnpaidOrdersDesc(eq(TENANT_ID), any(), any(), anyBoolean(), any(), any(),
                any(Pageable.class));
        verify(orderRepository).sumUnpaidBalance(eq(TENANT_ID), any(), any(), anyBoolean(), any(), any());
        assertThat(listDescQuery()).contains("o.tenant.id = :tenantId");
    }

    @Test
    void extensionOrdersWithZeroPriceNeverAppear() throws NoSuchMethodException {
        // balance 0 - 0 = 0 fails the strict > 0 predicate
        assertThat(listDescQuery()).contains(OrderRepository.UNPAID_BALANCE + " > 0");
    }

    // ---- date range ----

    @Test
    void fromAndToFilterOnOrderCreationDateInclusively() throws NoSuchMethodException {
        LocalDate from = LocalDate.parse("2026-09-01");
        LocalDate to = LocalDate.parse("2026-09-30");

        service.getUnpaidOrders(from, to, null, null, null, null);

        verify(orderRepository).findUnpaidOrdersDesc(eq(TENANT_ID), eq(from.atStartOfDay()),
                eq(to.atTime(LocalTime.MAX)), eq(false), eq(from), eq(to), any(Pageable.class));
        assertThat(to.atTime(LocalTime.MAX)).isAfter(LocalDateTime.parse("2026-09-30T23:59:59"));
        assertThat(listDescQuery()).contains("o.creationTime >= :from").contains("o.creationTime <= :to");
    }

    @Test
    void fromAfterToIsRejectedOnUnpaidOrders() {
        assertThatThrownBy(() -> service.getUnpaidOrders(LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-09-01"), null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(orderRepository, never()).findUnpaidOrdersDesc(any(), any(), any(), anyBoolean(), any(), any(),
                any(Pageable.class));
    }

    // ---- withoutPaymentsInRange ----

    @Test
    void withoutPaymentsInRangeKeepsOnlyOrdersWithNoPaymentInTheRange() throws NoSuchMethodException {
        LocalDate from = LocalDate.parse("2026-09-01");
        LocalDate to = LocalDate.parse("2026-09-30");

        service.getUnpaidOrders(from, to, true, null, null, null);

        // payment bounds are the plain LocalDate range, compared against paymentDate in a NOT EXISTS subquery
        verify(orderRepository).findUnpaidOrdersDesc(eq(TENANT_ID), any(), any(), eq(true), eq(from), eq(to),
                any(Pageable.class));
        assertThat(OrderRepository.UNPAID_NO_PAYMENTS_IN_RANGE).contains("NOT EXISTS")
                .contains("px.paymentDate >= :paymentFrom").contains("px.paymentDate <= :paymentTo");
        assertThat(listDescQuery()).contains(OrderRepository.UNPAID_NO_PAYMENTS_IN_RANGE);
    }

    @Test
    void withoutPaymentsInRangeWithNoRangeMeansOrdersWithNoPaymentsAtAll() {
        service.getUnpaidOrders(null, null, true, null, null, null);

        verify(orderRepository).findUnpaidOrdersDesc(eq(TENANT_ID), eq(null), eq(null), eq(true), eq(null),
                eq(null), any(Pageable.class));
        assertThat(OrderRepository.UNPAID_NO_PAYMENTS_IN_RANGE)
                .contains(":paymentFrom IS NULL").contains(":paymentTo IS NULL");
    }

    @Test
    void withoutPaymentsInRangeStillRequiresAPositiveBalance() throws NoSuchMethodException {
        // the balance predicate lives in the shared base WHERE; the NOT EXISTS clause is only appended to it
        for (String name : List.of("findUnpaidOrdersDesc", "sumUnpaidBalance")) {
            assertThat(queryOf(name)).contains(OrderRepository.UNPAID_BASE_WHERE + OrderRepository.UNPAID_NO_PAYMENTS_IN_RANGE);
        }
    }

    // ---- last payment / count ----

    @Test
    void lastPaymentDateAndPaymentCountAreAllTimePerOrder() throws NoSuchMethodException {
        UnpaidOrderRowDto source = new UnpaidOrderRowDto(1L, "o", "t", OrderStatus.PENDING, OrderExecutionStatus.ACTIVE,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"), LocalDate.parse("2026-03-07"), 2L);
        stub(List.of(source), 1, "90");

        UnpaidOrderRowDto result = service.getUnpaidOrders(LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-30"), null, null, null, null).data.get(0);

        assertThat(result.lastPaymentDate()).isEqualTo(LocalDate.parse("2026-03-07"));
        assertThat(result.paymentCount()).isEqualTo(2L);
        // the correlated subqueries carry no date predicate
        assertThat(OrderRepository.UNPAID_SELECT)
                .contains("(SELECT MAX(pl.paymentDate) FROM Payment pl WHERE pl.order = o)")
                .contains("(SELECT COUNT(pc) FROM Payment pc WHERE pc.order = o)");
    }

    @Test
    void orderWithoutPaymentsHasNullLastPaymentDateAndZeroCount() {
        stub(List.of(row(1, "100", "0", "100")), 1, "100");

        UnpaidOrderRowDto result = unpaid().data.get(0);

        assertThat(result.lastPaymentDate()).isNull();
        assertThat(result.paymentCount()).isZero();
    }

    // ---- sort ----

    @Test
    void defaultSortIsBalanceDescendingWithOrderIdTieBreak() throws NoSuchMethodException {
        unpaid();

        verify(orderRepository).findUnpaidOrdersDesc(any(), any(), any(), anyBoolean(), any(), any(),
                any(Pageable.class));
        verify(orderRepository, never()).findUnpaidOrdersAsc(any(), any(), any(), anyBoolean(), any(), any(),
                any(Pageable.class));
        assertThat(listDescQuery()).endsWith("ORDER BY " + OrderRepository.UNPAID_BALANCE + " DESC, o.id DESC");
    }

    @Test
    void ascendingSort() throws NoSuchMethodException {
        service.getUnpaidOrders(null, null, null, null, null, "asc");

        verify(orderRepository).findUnpaidOrdersAsc(any(), any(), any(), anyBoolean(), any(), any(),
                any(Pageable.class));
        verify(orderRepository, never()).findUnpaidOrdersDesc(any(), any(), any(), anyBoolean(), any(), any(),
                any(Pageable.class));
        assertThat(queryOf("findUnpaidOrdersAsc")).endsWith("ORDER BY " + OrderRepository.UNPAID_BALANCE + " ASC, o.id ASC");
    }

    // ---- totals / shape ----

    @Test
    void totalsCoverTheWholeFilteredSet() {
        List<UnpaidOrderRowDto> pageRows = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            pageRows.add(row(i, "10", "0", "10"));
        }
        stub(pageRows, 120, "1200.0000");

        UnpaidOrdersReportDto result = service.getUnpaidOrders(null, null, null, 1, 50, null);

        assertThat(result.data).hasSize(50);
        assertThat(result.totalElements).isEqualTo(120);
        assertThat(result.total).isEqualTo(3);
        assertMoney(result.totals.outstanding(), "1200.00");
        assertThat(result.totals.count()).isEqualTo(120);
    }

    @Test
    void rowShapeAndCurrency() {
        stub(List.of(row(7, "100", "20", "80")), 1, "80");

        UnpaidOrdersReportDto result = unpaid();

        UnpaidOrderRowDto r = result.data.get(0);
        assertThat(r.orderId()).isEqualTo(7L);
        assertThat(r.orderName()).isEqualTo("Order 7");
        assertThat(r.trackingId()).isEqualTo("trk7");
        assertThat(r.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(r.executionStatus()).isEqualTo(OrderExecutionStatus.ACTIVE);
        assertMoney(r.salePrice(), "100.00");
        assertMoney(r.amountPaid(), "20.00");
        assertMoney(r.amountLeftToPay(), "80.00");
        assertThat(r.lastPaymentDate()).isNull();
        assertThat(r.paymentCount()).isZero();
        assertThat(result.currency).isEqualTo("RSD");
    }

    @ParameterizedTest
    @CsvSource({"-1,50", "0,0", "0,101"})
    void invalidPaginationIsRejectedOnUnpaidOrders(int page, int perPage) {
        assertThatThrownBy(() -> service.getUnpaidOrders(null, null, null, page, perPage, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void perPage1IsAcceptedOnUnpaidOrders() {
        UnpaidOrdersReportDto result = service.getUnpaidOrders(null, null, null, null, 1, null);

        assertThat(result.perPage).isEqualTo(1);
    }

    @Test
    void unpaidOrdersPagingDefaults() {
        UnpaidOrdersReportDto result = unpaid();

        assertThat(result.page).isZero();
        assertThat(result.perPage).isEqualTo(50);
    }

    private static String listBalanceExpression() {
        return OrderRepository.UNPAID_BALANCE;
    }
}

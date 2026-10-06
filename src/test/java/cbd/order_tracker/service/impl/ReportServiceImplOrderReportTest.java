package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.dto.response.OrderReportDto;
import cbd.order_tracker.model.dto.response.UnpaidOrdersReportDto;
import cbd.order_tracker.repository.OrderRepository;
import cbd.order_tracker.repository.OrderStatusHistoryRepository;
import cbd.order_tracker.repository.PaymentRepository;
import cbd.order_tracker.util.UserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Order report card scenarios (totalOutstanding, and totalAmountPaid per the confirmed redefinition).
 * Repositories are mocked, so true numeric agreement with the unpaid list needs a real database.
 */
class ReportServiceImplOrderReportTest {

    private static final Long TENANT_ID = 1L;
    private static final LocalDate FROM = LocalDate.parse("2026-09-01");
    private static final LocalDate TO = LocalDate.parse("2026-09-30");

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private UserUtil userUtil;
    private ReportServiceImpl service;

    @BeforeEach
    void setup() {
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        userUtil = mock(UserUtil.class);
        service = new ReportServiceImpl(orderRepository, mock(OrderStatusHistoryRepository.class),
                paymentRepository, userUtil);
        TenantContext.setSuperadmin(false);
        TenantContext.setTenantId(TENANT_ID);
        asRole("company_admin");
        // [count, acquisitionCost, avgAcquisitionCost, amountPaid(old), salePrice, amountLeftToPay(old), ext, regular]
        when(orderRepository.getOrderReport(any(), any(), any())).thenReturn(new Object[]{
                new Object[]{5L, new BigDecimal("300.0000"), new BigDecimal("60.0000"), new BigDecimal("9999.0000"),
                        new BigDecimal("1000.0000"), new BigDecimal("8888.0000"), 2L, 3L}});
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private void asRole(String role) {
        when(userUtil.getCurrentUserRoles()).thenReturn(Set.of(new Role(role)));
    }

    private static String queryOf(Class<?> repo, String name) {
        for (Method m : repo.getMethods()) {
            if (m.getName().equals(name)) {
                return m.getAnnotation(Query.class).value();
            }
        }
        throw new IllegalArgumentException(name);
    }

    // ---- totalOutstanding ----

    @Test
    void totalOutstandingEqualsTheUnpaidListTotalForTheSameRange() {
        // both are fed from the same mocked repository value
        when(orderRepository.getOutstandingTotal(any(), any(), any())).thenReturn(new BigDecimal("1500.0000"));
        when(orderRepository.sumUnpaidBalance(any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(new BigDecimal("1500.0000"));
        when(orderRepository.findUnpaidOrdersDesc(any(), any(), any(), anyBoolean(), any(), any(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(6, Pageable.class), 0));

        OrderReportDto card = service.getOrderReport(FROM, TO);
        UnpaidOrdersReportDto list = service.getUnpaidOrders(FROM, TO, null, null, null, null);

        assertThat(card.getTotalOutstanding()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(card.getTotalOutstanding()).isEqualTo(list.totals.outstanding());
        verify(orderRepository).getOutstandingTotal(FROM.atStartOfDay(), TO.atTime(LocalTime.MAX), TENANT_ID);
        // same WHERE and balance expression: both queries are built from the same constants
        assertThat(queryOf(OrderRepository.class, "getOutstandingTotal"))
                .contains(OrderRepository.UNPAID_BASE_WHERE).contains(OrderRepository.UNPAID_BALANCE);
        assertThat(queryOf(OrderRepository.class, "sumUnpaidBalance"))
                .contains(OrderRepository.UNPAID_BASE_WHERE).contains(OrderRepository.UNPAID_BALANCE);
    }

    @Test
    void totalOutstandingSumsVatAwareBalancesFlooredAtZero() {
        // legal entity 800, overpaid -50 (floored out by balance > 0), individual 100 => 900
        when(orderRepository.getOutstandingTotal(any(), any(), any())).thenReturn(new BigDecimal("900.0000"));

        OrderReportDto card = service.getOrderReport(null, null);

        assertThat(card.getTotalOutstanding()).isEqualTo(new BigDecimal("900.00"));
        assertThat(queryOf(OrderRepository.class, "getOutstandingTotal"))
                .contains("o.salePriceWithTax").contains("o.legalEntity = true").contains(" > 0");
    }

    @Test
    void totalOutstandingUsesTheCreationDateRangeNotTheDoneDate() {
        when(orderRepository.getOutstandingTotal(any(), any(), any())).thenReturn(new BigDecimal("200.0000"));

        OrderReportDto card = service.getOrderReport(FROM, TO);

        assertThat(card.getTotalOutstanding()).isEqualTo(new BigDecimal("200.00"));
        String q = queryOf(OrderRepository.class, "getOutstandingTotal");
        assertThat(q).contains("o.creationTime >= :from").contains("o.creationTime <= :to")
                .doesNotContain("dateWhenMovedToDone");
    }

    @Test
    void totalOutstandingExcludesCanceledAndSoftDeletedOrders() {
        String q = queryOf(OrderRepository.class, "getOutstandingTotal");

        assertThat(q).contains("o.deleted = false")
                .contains("o.tenant.id = :tenantId")
                .contains("o.executionStatus <> cbd.order_tracker.model.OrderExecutionStatus.CANCELED");
    }

    @Test
    void totalOutstandingIsRoundedHalfUpToTwoDecimals() {
        when(orderRepository.getOutstandingTotal(any(), any(), any())).thenReturn(new BigDecimal("120.012"));
        assertThat(service.getOrderReport(null, null).getTotalOutstanding()).isEqualTo(new BigDecimal("120.01"));

        when(orderRepository.getOutstandingTotal(any(), any(), any())).thenReturn(new BigDecimal("120.005"));
        assertThat(service.getOrderReport(null, null).getTotalOutstanding()).isEqualTo(new BigDecimal("120.01"));
    }

    @Test
    void otherFieldsOfTheOrderReportKeepTheirNumbersAndDateFilter() {
        OrderReportDto card = service.getOrderReport(FROM, TO);

        assertThat(card.getOrderCount()).isEqualTo(5L);
        assertThat(card.getTotalAcquisitionCost()).isEqualByComparingTo("300");
        assertThat(card.getAverageAcquisitionCost()).isEqualByComparingTo("60");
        assertThat(card.getTotalSalePrice()).isEqualByComparingTo("1000");
        assertThat(card.getExtensionOrderCount()).isEqualTo(2L);
        assertThat(card.getRegularOrderCount()).isEqualTo(3L);
        assertThat(card.getProfitMargin()).isEqualByComparingTo("700");
        verify(orderRepository).getOrderReport(FROM.atStartOfDay(), TO.atTime(LocalTime.MAX), TENANT_ID);
        String q = queryOf(OrderRepository.class, "getOrderReport");
        assertThat(q).contains("o.dateWhenMovedToDone >= :from").contains("o.dateWhenMovedToDone <= :to")
                .contains("o.executionStatus <> cbd.order_tracker.model.OrderExecutionStatus.CANCELED");
    }

    @Test
    void totalOutstandingStaysAdminOnly() {
        asRole("manager");

        OrderReportDto card = service.getOrderReport(FROM, TO);

        assertThat(card.getTotalOutstanding()).isNull();
        assertThat(card.getTotalAmountPaid()).isNull();
        verify(orderRepository, never()).getOutstandingTotal(any(), any(), any());
    }

    @Test
    void bothBoundsOmittedMeansAllNonCanceledNonDeletedOrders() {
        service.getOrderReport(null, null);

        verify(orderRepository).getOutstandingTotal(eq(null), eq(null), eq(TENANT_ID));
    }

    // ---- totalAmountPaid redefinition (confirmed) ----

    @Test
    void totalAmountPaidEqualsPaymentsOverallForTheSameRange() {
        when(paymentRepository.sumPaymentAmounts(any(), any(), any())).thenReturn(new BigDecimal("5000.00"));
        when(paymentRepository.sumPaymentsByMethod(any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{null, new BigDecimal("5000.00"), 1L}));
        when(paymentRepository.findPaymentReportRows(any(), any(), any(), anyBoolean(), anyBoolean(), any(),
                anyBoolean(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(7, Pageable.class), 0));

        OrderReportDto card = service.getOrderReport(FROM, TO);
        BigDecimal overall = service.getPaymentsReport(FROM, TO, null, null, null, null).totals.overall();

        assertThat(card.getTotalAmountPaid()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(card.getTotalAmountPaid()).isEqualTo(overall);
        // plain LocalDate bounds, same as the payments report
        verify(paymentRepository).sumPaymentAmounts(TENANT_ID, FROM, TO);
    }

    @Test
    void totalAmountPaidUsesPaymentDateNotOrderDoneDate() {
        String q = queryOf(PaymentRepository.class, "sumPaymentAmounts");

        assertThat(q).contains("p.paymentDate >= :from").contains("p.paymentDate <= :to")
                .doesNotContain("dateWhenMovedToDone").doesNotContain("status");
        // the old per-order sum is no longer used for the card
        assertThat(service.getOrderReport(FROM, TO).getTotalAmountPaid()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void totalAmountPaidIncludesCanceledOrdersPaymentsAndExcludesSoftDeletedOnes() {
        String q = queryOf(PaymentRepository.class, "sumPaymentAmounts");

        assertThat(q).contains("JOIN p.order o").contains("o.deleted = false").contains("o.tenant.id = :tenantId")
                .doesNotContain("executionStatus");
    }

    @Test
    void totalAmountPaidStaysAdminOnly() {
        asRole("manufacturer");

        OrderReportDto card = service.getOrderReport(FROM, TO);

        assertThat(card.getTotalAmountPaid()).isNull();
        verify(paymentRepository, never()).sumPaymentAmounts(any(), any(), any());
    }

    @Test
    void profitAndOtherFieldsAreUnaffectedByTheTotalAmountPaidRedefinition() {
        when(paymentRepository.sumPaymentAmounts(any(), any(), any())).thenReturn(new BigDecimal("123456.00"));

        OrderReportDto card = service.getOrderReport(null, null);

        assertThat(card.getTotalSalePrice()).isEqualByComparingTo("1000");
        assertThat(card.getTotalAcquisitionCost()).isEqualByComparingTo("300");
        assertThat(card.getAverageAcquisitionCost()).isEqualByComparingTo("60");
        assertThat(card.getProfitMargin()).isEqualByComparingTo("700");
        assertThat(card.getOrderCount()).isEqualTo(5L);
    }
}

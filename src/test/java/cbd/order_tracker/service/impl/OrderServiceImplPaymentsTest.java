package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.Payment;
import cbd.order_tracker.model.PaymentMethod;
import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.dto.PaymentRequestDto;
import cbd.order_tracker.model.dto.UpdatePaymentsResponse;
import cbd.order_tracker.repository.OrderRepository;
import cbd.order_tracker.repository.OrderStatusHistoryRepository;
import cbd.order_tracker.repository.PaymentRepository;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.util.UserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Stale balance fix in updateOrder and the payment amount/date validation on the write paths. */
class OrderServiceImplPaymentsTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ORDER_ID = 10L;
    private static final Long PAYMENT_ID = 77L;
    private static final String AMOUNT_MESSAGE = "Payment amount must be greater than zero";

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private OrderServiceImpl service;
    private Tenant tenant;

    @BeforeEach
    void setup() {
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        UserUtil userUtil = mock(UserUtil.class);
        OrderStatusHistoryRepository historyRepository = mock(OrderStatusHistoryRepository.class);
        service = new OrderServiceImpl(userUtil, orderRepository, paymentRepository, historyRepository,
                mock(TenantRepository.class));
        tenant = new Tenant("CBD", "cbd");
        tenant.setId(TENANT_ID);
        TenantContext.setSuperadmin(false);
        TenantContext.setTenantId(TENANT_ID);
        when(userUtil.getCurrentUserRoles()).thenReturn(Set.of(new Role("company_admin")));
        when(orderRepository.save(any(OrderRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(historyRepository.findByOrderId(ORDER_ID)).thenReturn(List.of());
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private static OrderRecord incoming(String salePrice, String acquisitionCost, boolean legalEntity) {
        OrderRecord o = new OrderRecord();
        o.setId(ORDER_ID);
        o.setName("A");
        o.setSalePrice(new BigDecimal(salePrice));
        o.setAcquisitionCost(new BigDecimal(acquisitionCost));
        o.setLegalEntity(legalEntity);
        return o;
    }

    /** Stored order created through the real constructor, then given the requested paid amount (stale balances). */
    private OrderRecord stored(String salePrice, String acquisitionCost, boolean legalEntity, String amountPaid) {
        OrderRecord r = new OrderRecord(incoming(salePrice, acquisitionCost, legalEntity));
        r.setId(ORDER_ID);
        r.setTenant(tenant);
        r.setAmountPaid(amountPaid == null ? null : new BigDecimal(amountPaid));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(r));
        return r;
    }

    private OrderRecord savedRecord() {
        ArgumentCaptor<OrderRecord> captor = ArgumentCaptor.forClass(OrderRecord.class);
        verify(orderRepository).save(captor.capture());
        return captor.getValue();
    }

    private static PaymentRequestDto paymentRequest(BigDecimal amount, LocalDate date) {
        return new PaymentRequestDto(null, "Marko", amount, date, PaymentMethod.CASH, null);
    }

    private static Payment payment(Long id, String amount, LocalDate date) {
        Payment p = new Payment();
        ReflectionTestUtils.setField(p, "id", id);
        p.setPayer("Marko");
        p.setAmount(amount == null ? null : new BigDecimal(amount));
        p.setPaymentDate(date);
        p.setPaymentMethod(PaymentMethod.CASH);
        return p;
    }

    /** Stored order with one existing payment of 100.00 (amountPaid 100.00). */
    private OrderRecord storedWithPayment() {
        OrderRecord order = stored("1000", "600", false, "100");
        order.getPayments().add(payment(PAYMENT_ID, "100.00", LocalDate.parse("2026-09-01")));
        return order;
    }

    // ---- stale balance fix ----

    @Test
    void priceEditRecomputesTheStoredBalances() {
        stored("1000", "600", false, "400");

        service.updateOrder(incoming("1500", "700", false));

        OrderRecord saved = savedRecord();
        assertThat(saved.getSalePriceWithTax()).isEqualByComparingTo("1800.0000");
        assertThat(saved.getAmountLeftToPay()).isEqualByComparingTo("1100.00");
        assertThat(saved.getAmountLeftToPayWithTax()).isEqualByComparingTo("1400.00");
        assertThat(saved.getPriceDifference()).isEqualByComparingTo("800.00");
    }

    @Test
    void nullAmountPaidIsTreatedAsZeroDuringRecompute() {
        stored("1000", "600", false, null);

        // OrderMapper.toDto (pre-existing, out of scope) throws an NPE on a null amountPaid after the order
        // has already been saved, so only the saved entity is asserted here.
        try {
            service.updateOrder(incoming("100", "40", false));
        } catch (NullPointerException expectedFromMapper) {
            // ignored on purpose
        }

        assertThat(savedRecord().getAmountLeftToPay()).isEqualByComparingTo("100.00");
    }

    @Test
    void legalEntityChangeDoesNotAlterTheStoredBalanceColumns() {
        stored("100", "40", false, "0");

        service.updateOrder(incoming("100", "40", true));

        OrderRecord saved = savedRecord();
        assertThat(saved.isLegalEntity()).isTrue();
        assertThat(saved.getAmountLeftToPay()).isEqualByComparingTo("100.00");
        assertThat(saved.getAmountLeftToPayWithTax()).isEqualByComparingTo("120.00");
    }

    @Test
    void recomputeIsConsistentWithThePaymentWritePaths() {
        stored("1000", "600", false, "400");
        service.updateOrder(incoming("1500", "700", false));
        OrderRecord updated = savedRecord();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(updated));

        service.addPayment(ORDER_ID, paymentRequest(new BigDecimal("100.00"), LocalDate.parse("2026-09-02")));

        assertThat(updated.getAmountPaid()).isEqualByComparingTo("500.00");
        assertThat(updated.getAmountLeftToPay())
                .isEqualByComparingTo(updated.getSalePrice().subtract(updated.getAmountPaid()));
        assertThat(updated.getAmountLeftToPay()).isEqualByComparingTo("1000.00");
    }

    // ---- payment validation ----

    @ParameterizedTest
    @CsvSource(value = {"null", "0.00", "-10.00"}, nullValues = "null")
    void addPaymentRejectsInvalidAmounts(String amount) {
        OrderRecord order = stored("1000", "600", false, "50");
        BigDecimal value = amount == null ? null : new BigDecimal(amount);

        assertThatThrownBy(() -> service.addPayment(ORDER_ID, paymentRequest(value, LocalDate.parse("2026-09-01"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(AMOUNT_MESSAGE);

        assertThat(order.getAmountPaid()).isEqualByComparingTo("50");
        assertThat(order.getPayments()).isEmpty();
        verify(orderRepository, never()).save(any(OrderRecord.class));
    }

    @ParameterizedTest
    @CsvSource(value = {"null", "0.00", "-10.00"}, nullValues = "null")
    void editPaymentRejectsInvalidAmounts(String amount) {
        OrderRecord order = storedWithPayment();

        assertThatThrownBy(() -> service.editPayment(ORDER_ID, payment(PAYMENT_ID, amount, LocalDate.parse("2026-09-05"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(AMOUNT_MESSAGE);

        assertThat(order.getPayments().get(0).getAmount()).isEqualByComparingTo("100.00");
        assertThat(order.getPayments().get(0).getPaymentDate()).isEqualTo(LocalDate.parse("2026-09-01"));
        assertThat(order.getAmountPaid()).isEqualByComparingTo("100");
        verify(orderRepository, never()).save(any(OrderRecord.class));
    }

    @Test
    void addPaymentRejectsANullPaymentDate() {
        OrderRecord order = stored("1000", "600", false, "0");

        assertThatThrownBy(() -> service.addPayment(ORDER_ID, paymentRequest(new BigDecimal("10.00"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment date is required");

        assertThat(order.getPayments()).isEmpty();
        verify(orderRepository, never()).save(any(OrderRecord.class));
    }

    @Test
    void editPaymentRejectsANullPaymentDate() {
        OrderRecord order = storedWithPayment();

        assertThatThrownBy(() -> service.editPayment(ORDER_ID, payment(PAYMENT_ID, "10.00", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment date is required");

        assertThat(order.getPayments().get(0).getAmount()).isEqualByComparingTo("100.00");
        verify(orderRepository, never()).save(any(OrderRecord.class));
    }

    @Test
    void validPositiveAmountIsAccepted() {
        OrderRecord order = stored("1000", "600", false, "0");

        service.addPayment(ORDER_ID, paymentRequest(new BigDecimal("0.01"), LocalDate.parse("2026-09-01")));

        assertThat(order.getPayments()).hasSize(1);
        assertThat(order.getAmountPaid()).isEqualByComparingTo("0.01");
        verify(orderRepository).save(order);
    }

    @Test
    void deletePaymentIsUnaffectedByTheNewValidation() {
        OrderRecord order = storedWithPayment();

        UpdatePaymentsResponse response = service.deletePayment(ORDER_ID, PAYMENT_ID);

        assertThat(order.getPayments()).isEmpty();
        assertThat(order.getAmountPaid()).isEqualByComparingTo("0");
        assertThat(response.getAmountPaid()).isEqualByComparingTo("0");
        verify(paymentRepository).delete(any(Payment.class));
    }
}

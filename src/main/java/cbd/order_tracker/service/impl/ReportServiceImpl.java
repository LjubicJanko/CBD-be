package cbd.order_tracker.service.impl;

import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.*;
import cbd.order_tracker.model.dto.response.*;
import cbd.order_tracker.repository.OrderRepository;
import cbd.order_tracker.repository.OrderStatusHistoryRepository;
import cbd.order_tracker.repository.PaymentRepository;
import cbd.order_tracker.service.ReportService;
import cbd.order_tracker.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final UserUtil userUtil;

    private static final String CURRENCY = "RSD";
    private static final int DEFAULT_PER_PAGE = 50;
    private static final int MAX_PER_PAGE = 100;
    private static final List<String> BY_METHOD_KEYS = List.of("ACCOUNT", "CASH", "ON_SHIP", "INVOICE", "UNSPECIFIED");
    private static final String UNSPECIFIED = "UNSPECIFIED";

    private static BigDecimal round2(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
    }

    private static int validatedPage(Integer page) {
        int value = page != null ? page : 0;
        if (value < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        return value;
    }

    private static int validatedPerPage(Integer perPage) {
        int value = perPage != null ? perPage : DEFAULT_PER_PAGE;
        if (value < 1 || value > MAX_PER_PAGE) {
            throw new IllegalArgumentException("perPage must be between 1 and " + MAX_PER_PAGE);
        }
        return value;
    }

    private static boolean isAscending(String sort) {
        return sort != null && "asc".equalsIgnoreCase(sort.trim());
    }

    @Override
    public PaymentsReportDto getPaymentsReport(LocalDate from, LocalDate to, String methods,
                                               Integer page, Integer perPage, String sort) {
        validateRange(from, to);
        int pageNumber = validatedPage(page);
        int pageSize = validatedPerPage(perPage);

        Set<String> selected = parseMethods(methods);
        boolean methodFilter = !selected.isEmpty();
        boolean includeUnspecified = selected.contains(UNSPECIFIED);
        List<PaymentMethod> methodEnums = selected.stream()
                .filter(m -> !UNSPECIFIED.equals(m))
                .map(PaymentMethod::valueOf)
                .toList();
        boolean hasMethods = !methodEnums.isEmpty();
        // the IN list must never be empty; it is ignored when hasMethods is false
        List<PaymentMethod> methodsParam = hasMethods ? methodEnums : List.of(PaymentMethod.CASH);

        Sort.Direction direction = isAscending(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "paymentDate", "id"));
        Long tenantId = TenantContext.requireTenantId();

        Page<PaymentReportRowDto> rows = paymentRepository.findPaymentReportRows(
                tenantId, from, to, methodFilter, hasMethods, methodsParam, includeUnspecified, pageable);

        // unrounded sums/counts per method over the date range (ignores the methods filter)
        Map<String, BigDecimal> sums = new HashMap<>();
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : paymentRepository.sumPaymentsByMethod(tenantId, from, to)) {
            String key = row[0] == null ? UNSPECIFIED : ((PaymentMethod) row[0]).name();
            BigDecimal sum = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
            long count = row[2] == null ? 0L : ((Number) row[2]).longValue();
            sums.merge(key, sum, BigDecimal::add);
            counts.merge(key, count, Long::sum);
        }

        Map<String, BigDecimal> byMethod = new LinkedHashMap<>();
        for (String key : BY_METHOD_KEYS) {
            byMethod.put(key, round2(sums.get(key)));
        }
        BigDecimal bankTotal = round2(sums.getOrDefault("ACCOUNT", BigDecimal.ZERO)
                .add(sums.getOrDefault("INVOICE", BigDecimal.ZERO))
                .add(sums.getOrDefault("ON_SHIP", BigDecimal.ZERO)));

        BigDecimal overall = BigDecimal.ZERO;
        long count = 0;
        for (String key : BY_METHOD_KEYS) {
            if (!methodFilter || selected.contains(key)) {
                overall = overall.add(sums.getOrDefault(key, BigDecimal.ZERO));
                count += counts.getOrDefault(key, 0L);
            }
        }

        PaymentsTotalsDto totals = new PaymentsTotalsDto(round2(overall), count, bankTotal, byMethod);
        return new PaymentsReportDto(pageNumber, pageSize, rows.getTotalPages(), rows.getTotalElements(),
                rows.getContent(), CURRENCY, totals);
    }

    private static Set<String> parseMethods(String methods) {
        Set<String> result = new LinkedHashSet<>();
        if (methods == null || methods.isBlank()) {
            return result;
        }
        for (String token : methods.split(",")) {
            String name = token.trim().toUpperCase(Locale.ROOT);
            if (!BY_METHOD_KEYS.contains(name)) {
                throw new IllegalArgumentException("Unknown payment method: " + token.trim());
            }
            result.add(name);
        }
        return result;
    }

    @Override
    public UnpaidOrdersReportDto getUnpaidOrders(LocalDate from, LocalDate to, Boolean withoutPaymentsInRange,
                                                 Integer page, Integer perPage, String sort) {
        validateRange(from, to);
        int pageNumber = validatedPage(page);
        int pageSize = validatedPerPage(perPage);

        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;
        boolean withoutPayments = Boolean.TRUE.equals(withoutPaymentsInRange);
        Long tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<UnpaidOrderRowDto> rows = isAscending(sort)
                ? orderRepository.findUnpaidOrdersAsc(tenantId, fromDateTime, toDateTime, withoutPayments, from, to, pageable)
                : orderRepository.findUnpaidOrdersDesc(tenantId, fromDateTime, toDateTime, withoutPayments, from, to, pageable);
        BigDecimal outstanding = orderRepository.sumUnpaidBalance(
                tenantId, fromDateTime, toDateTime, withoutPayments, from, to);

        List<UnpaidOrderRowDto> data = rows.getContent().stream()
                .map(r -> new UnpaidOrderRowDto(r.orderId(), r.orderName(), r.trackingId(), r.status(),
                        r.executionStatus(), round2(r.salePrice()), round2(r.amountPaid()),
                        round2(r.amountLeftToPay()), r.lastPaymentDate(),
                        r.paymentCount() == null ? 0L : r.paymentCount()))
                .toList();

        return new UnpaidOrdersReportDto(pageNumber, pageSize, rows.getTotalPages(), rows.getTotalElements(), data,
                CURRENCY, new UnpaidTotalsDto(round2(outstanding), rows.getTotalElements()));
    }

    @Override
    public OrderReportDto getOrderReport(LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;

        Long tenantId = TenantContext.requireTenantId();
        OrderReportRaw raw = OrderReportRaw.from(
                orderRepository.getOrderReport(fromDateTime, toDateTime, tenantId));

        Set<Role> roles = userUtil.getCurrentUserRoles();
        boolean isAdmin = TenantContext.isSuperadmin() || roles.stream().anyMatch(role -> "company_admin".equals(role.getName()));

        OrderReportDto dto = new OrderReportDto();
        dto.setOrderCount(raw.orderCount());
        dto.setTotalAcquisitionCost(raw.totalAcquisitionCost());
        dto.setAverageAcquisitionCost(raw.avgAcquisitionCost());
        dto.setExtensionOrderCount(raw.extensionOrderCount());
        dto.setRegularOrderCount(raw.regularOrderCount());

        if (isAdmin) {
            // totalAmountPaid: sum of payments by payment date (equals payments report totals.overall)
            dto.setTotalAmountPaid(round2(paymentRepository.sumPaymentAmounts(tenantId, from, to)));
            dto.setTotalSalePrice(raw.totalSalePrice());
            // totalOutstanding: same definition as the unpaid-orders list (creation-date range)
            dto.setTotalOutstanding(round2(orderRepository.getOutstandingTotal(fromDateTime, toDateTime, tenantId)));
            dto.setProfitMargin(dto.getTotalSalePrice().subtract(dto.getTotalAcquisitionCost()));
        }

        return dto;
    }

    @Override
    public StatusDurationReportDto getStatusDurationReport(LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;

        List<OrderRecord> completedOrders = orderRepository.findCompletedOrders(fromDateTime, toDateTime, TenantContext.requireTenantId());

        List<Long> orderIds = completedOrders.stream().map(OrderRecord::getId).toList();
        Map<Long, List<OrderStatusHistory>> historyByOrderId = orderIds.isEmpty()
                ? Map.of()
                : statusHistoryRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(h -> h.getOrder().getId()));

        // Track total hours per status across all orders
        Map<OrderStatus, Double> totalHoursPerStatus = new EnumMap<>(OrderStatus.class);
        Map<OrderStatus, Long> orderCountPerStatus = new EnumMap<>(OrderStatus.class);

        for (OrderRecord order : completedOrders) {
            List<OrderStatusHistory> history = historyByOrderId.getOrDefault(order.getId(), List.of());

            // Split history into status transitions and pause/unpause events
            List<OrderStatusHistory> statusEntries = history.stream()
                    .filter(h -> h.getStatus() != null)
                    .sorted(Comparator.comparing(OrderStatusHistory::getCreationTime))
                    .toList();

            List<OrderStatusHistory> pauseEvents = history.stream()
                    .filter(h -> h.getExecutionStatus() != null)
                    .sorted(Comparator.comparing(OrderStatusHistory::getCreationTime))
                    .toList();

            for (int i = 0; i < statusEntries.size(); i++) {
                OrderStatusHistory current = statusEntries.get(i);
                if (current.getStatus() == OrderStatus.DONE) continue;

                LocalDateTime start = current.getCreationTime();
                LocalDateTime end = (i + 1 < statusEntries.size())
                        ? statusEntries.get(i + 1).getCreationTime()
                        : order.getDateWhenMovedToDone();

                if (start != null && end != null) {
                    double pausedMinutes = calculatePausedMinutes(pauseEvents, start, end);
                    double hours = (ChronoUnit.MINUTES.between(start, end) - pausedMinutes) / 60.0;
                    if (hours < 0) hours = 0;
                    totalHoursPerStatus.merge(current.getStatus(), hours, Double::sum);
                    orderCountPerStatus.merge(current.getStatus(), 1L, Long::sum);
                }
            }
        }

        // Calculate averages
        Map<OrderStatus, Double> avgHoursPerStatus = new EnumMap<>(OrderStatus.class);
        double totalAvgHours = 0;

        for (OrderStatus status : totalHoursPerStatus.keySet()) {
            double avg = totalHoursPerStatus.get(status) / orderCountPerStatus.get(status);
            avgHoursPerStatus.put(status, avg);
            totalAvgHours += avg;
        }

        // Build response with percentages
        List<StatusDurationDto> durations = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            if (status == OrderStatus.DONE) continue;
            Double avgHours = avgHoursPerStatus.get(status);
            if (avgHours != null) {
                double percentage = totalAvgHours > 0 ? (avgHours / totalAvgHours) * 100 : 0;
                durations.add(new StatusDurationDto(status,
                        Math.round(avgHours * 100.0) / 100.0,
                        Math.round(percentage * 100.0) / 100.0));
            }
        }

        return new StatusDurationReportDto((long) completedOrders.size(), durations);
    }

    /**
     * Calculate total paused minutes within a time window.
     * Pairs PAUSED -> ACTIVE events and sums overlapping time with [windowStart, windowEnd].
     */
    private double calculatePausedMinutes(List<OrderStatusHistory> pauseEvents, LocalDateTime windowStart, LocalDateTime windowEnd) {
        double totalPausedMinutes = 0;
        LocalDateTime pausedAt = null;

        for (OrderStatusHistory event : pauseEvents) {
            if (event.getExecutionStatus() == OrderExecutionStatus.PAUSED) {
                pausedAt = event.getCreationTime();
            } else if (event.getExecutionStatus() == OrderExecutionStatus.ACTIVE && pausedAt != null) {
                LocalDateTime unpausedAt = event.getCreationTime();

                // Clamp to window boundaries
                LocalDateTime effectiveStart = pausedAt.isBefore(windowStart) ? windowStart : pausedAt;
                LocalDateTime effectiveEnd = unpausedAt.isAfter(windowEnd) ? windowEnd : unpausedAt;

                if (effectiveStart.isBefore(effectiveEnd)) {
                    totalPausedMinutes += ChronoUnit.MINUTES.between(effectiveStart, effectiveEnd);
                }
                pausedAt = null;
            }
        }

        return totalPausedMinutes;
    }
}

package cbd.order_tracker.service.impl;

import cbd.order_tracker.model.*;
import cbd.order_tracker.model.dto.response.OrderReportDto;
import cbd.order_tracker.model.dto.response.StatusDurationDto;
import cbd.order_tracker.model.dto.response.StatusDurationReportDto;
import cbd.order_tracker.repository.OrderRepository;
import cbd.order_tracker.repository.OrderStatusHistoryRepository;
import cbd.order_tracker.service.ReportService;
import cbd.order_tracker.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final UserUtil userUtil;

    @Override
    public OrderReportDto getOrderReport(LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;

        Object[] raw = orderRepository.getOrderReport(fromDateTime, toDateTime);
        Object[] result = (raw.length == 1 && raw[0] instanceof Object[]) ? (Object[]) raw[0] : raw;

        Set<Role> roles = userUtil.getCurrentUserRoles();
        boolean isAdmin = roles.stream().anyMatch(role -> "admin".equals(role.getName()));

        OrderReportDto dto = new OrderReportDto();
        dto.setOrderCount(((Number) result[0]).longValue());
        dto.setTotalAcquisitionCost(new BigDecimal(result[1].toString()));
        dto.setAverageAcquisitionCost(new BigDecimal(result[2].toString()));
        dto.setExtensionOrderCount(((Number) result[6]).longValue());
        dto.setRegularOrderCount(((Number) result[7]).longValue());

        if (isAdmin) {
            dto.setTotalAmountPaid(new BigDecimal(result[3].toString()));
            dto.setTotalSalePrice(new BigDecimal(result[4].toString()));
            dto.setTotalOutstanding(new BigDecimal(result[5].toString()));
            dto.setProfitMargin(dto.getTotalSalePrice().subtract(dto.getTotalAcquisitionCost()));
        }

        return dto;
    }

    @Override
    public StatusDurationReportDto getStatusDurationReport(LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;

        List<OrderRecord> completedOrders = orderRepository.findCompletedOrders(fromDateTime, toDateTime);

        // Track total hours per status across all orders
        Map<OrderStatus, Double> totalHoursPerStatus = new EnumMap<>(OrderStatus.class);
        Map<OrderStatus, Long> orderCountPerStatus = new EnumMap<>(OrderStatus.class);

        for (OrderRecord order : completedOrders) {
            List<OrderStatusHistory> history = statusHistoryRepository.findByOrderId(order.getId());

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

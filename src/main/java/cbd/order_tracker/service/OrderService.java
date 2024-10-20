package cbd.order_tracker.service;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.OrderStatusHistory;
import cbd.order_tracker.model.Payment;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.OrderOverviewDto;
import cbd.order_tracker.model.dto.OrderTrackingDTO;
import cbd.order_tracker.model.dto.PageableResponse;
import cbd.order_tracker.repository.OrderRepository;
import cbd.order_tracker.repository.OrderStatusHistoryRepository;
import cbd.order_tracker.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import cbd.order_tracker.util.OrderMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusHistoryRepository statusHistoryRepository;

    public OrderDTO createOrder(OrderRecord order) {
        OrderRecord newOrder = new OrderRecord(order);
        OrderRecord orderRecord = orderRepository.save(newOrder);
        return OrderMapper.toDto(orderRecord);
    }

    public OrderDTO updateOrder(OrderRecord order) {
        OrderRecord orderRecord = orderRepository.save(order);
        return OrderMapper.toDto(orderRecord);
    }

    /**
     * TODO FIX LOGIC WITH STATUS CHANGE FOR COMMENTS AND STUFF
     */
    public OrderDTO changeStatus(Long id, String closingComment, String postalCode, String postalService) {
        OrderRecord orderRecord = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        try {
            String currentUser = UserUtil.getCurrentUser();

            List<OrderStatusHistory> historyList = orderRecord.getStatusHistory();
            var lastHistoryRecord = historyList.get(historyList.size() - 1);
            lastHistoryRecord.setClosingComment(closingComment);
            lastHistoryRecord.setUser(currentUser);
            orderRecord.setStatusHistory(historyList);
            orderRecord.nextStatus(postalCode, postalService);

            orderRepository.save(orderRecord);
        } catch (IllegalStateException error) {
            System.out.println(error.getMessage());
        }
        return OrderMapper.toDto(orderRecord);
    }

    public OrderDTO addPayment(Long id, Payment payment) {
        OrderRecord orderRecord = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        orderRecord.addPayment(payment);
        orderRepository.save(orderRecord);

        return OrderMapper.toDto(orderRecord);
    }

    /**
     * Get OrderDTO by id
     *
     * @param id
     * @return
     */
    public OrderDTO getOrderById(Long id) {
        OrderRecord orderRecord = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return OrderMapper.toDto(orderRecord);
    }

    public PageableResponse<OrderOverviewDto> getAllPageable(List<OrderStatus> statuses, Integer page, Integer perPage) {
        Pageable pageRequest = PageRequest.of(page, perPage);

        Page<OrderRecord> orderRecords;

        if (statuses != null && !statuses.isEmpty()) {
            orderRecords = orderRepository.findAllByStatusIn(statuses, pageRequest);
        } else {
            orderRecords = orderRepository.findAll(pageRequest);
        }

        var orderOverviewDtos = orderRecords.stream()
                .map(OrderMapper::toOverviewDto)
                .collect(Collectors.toList());

        return new PageableResponse<OrderOverviewDto>(page, perPage, orderRecords.getTotalPages(), orderOverviewDtos);
    }

    public List<OrderDTO> getAll(List<OrderStatus> statuses) {
        Iterable<OrderRecord> orderRecords;

        if (statuses != null && !statuses.isEmpty()) {
            orderRecords = orderRepository.findByStatusIn(statuses);
        } else {
            orderRecords = orderRepository.findAll();
        }

        List<OrderRecord> orderRecordList = StreamSupport.stream(orderRecords.spliterator(), false)
                .collect(Collectors.toList());


        return orderRecordList.stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    public OrderTrackingDTO getOrderByTrackingId(String trackingId) {
        OrderRecord orderRecord = orderRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        List<OrderStatusHistory> history = getOrderStatusHistory(orderRecord.getId());
        orderRecord.setStatusHistory(history);
        return OrderMapper.toOrderTrackingDTO(orderRecord);

    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    public List<OrderStatusHistory> getOrderStatusHistory(Long orderId) {
        return statusHistoryRepository.findByOrderId(orderId);
    }

    public List<OrderOverviewDto> searchOrders(String searchTerm) {
        Iterable<OrderRecord> ordersIterable;

        if (searchTerm == null || searchTerm.isEmpty()) {
            // Get all orders if no search term is provided
            ordersIterable = orderRepository.findAll();
        } else {
            // Perform the search with the custom query method
            ordersIterable = orderRepository.findByNameContainingOrDescriptionContaining(searchTerm, searchTerm);
        }

        // Convert the Iterable to a List using StreamSupport
        List<OrderRecord> orders = StreamSupport
                .stream(ordersIterable.spliterator(), false)
                .collect(Collectors.toList());

        // Map to DTO and return
        return orders.stream()
                .map(OrderMapper::toOverviewDto)
                .collect(Collectors.toList());

    }
}

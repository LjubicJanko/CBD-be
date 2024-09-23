package cbd.order_tracker.service;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.OrderStatusHistory;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.repository.OrderRepository;
import cbd.order_tracker.repository.OrderStatusHistoryRepository;
import cbd.order_tracker.util.ResourceNotFound;
import cbd.order_tracker.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cbd.order_tracker.util.OrderMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
        String currentUser = UserUtil.getCurrentUser();
        OrderRecord newOrder = new OrderRecord(order.getName(), order.getDescription(), currentUser);
        OrderRecord orderRecord = orderRepository.save(newOrder);
        return OrderMapper.toDto(orderRecord);
    }

    public OrderDTO updateOrder(OrderRecord order) {
        OrderRecord orderRecord = orderRepository.save(order);
        return OrderMapper.toDto(orderRecord);
    }

    public OrderDTO changeStatus(Long id) {
        OrderRecord orderRecord = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        try {
            String currentUser = UserUtil.getCurrentUser();
            orderRecord.nextStatus(currentUser);
            orderRepository.save(orderRecord);
        } catch (IllegalStateException error) {
            System.out.println(error.getMessage());
        }
        return OrderMapper.toDto(orderRecord);
    }

    public OrderDTO getOrderById(Long id) {
        OrderRecord orderRecord = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return OrderMapper.toDto(orderRecord);
    }

    public List<OrderDTO> getAll(List<OrderStatus> statuses) {
        Iterable<OrderRecord> orderRecords;
        System.out.println(statuses);

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

    public OrderDTO getOrderByTrackingId(String trackingId) {
        OrderRecord orderRecord = orderRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        List<OrderStatusHistory> history = getOrderStatusHistory(orderRecord.getId());
        orderRecord.setStatusHistory(history);
        return OrderMapper.toDto(orderRecord);

    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    public OrderRecord changeOrderStatus(Long orderId, OrderStatus newStatus) {
        OrderRecord orderRecord = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        orderRecord.setStatus(newStatus);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(orderRecord);
        history.setStatus(newStatus);
        history.setTimestamp(LocalDateTime.now());

        statusHistoryRepository.save(history);
        return orderRepository.save(orderRecord);
    }

    public List<OrderStatusHistory> getOrderStatusHistory(Long orderId) {
        return statusHistoryRepository.findByOrderId(orderId);
    }

    public List<OrderDTO> searchOrders(String searchTerm) {
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
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());

    }
}

package cbd.order_tracker.controller;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController (OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        return new ResponseEntity<OrderDTO>(orderService.getOrderById(id), HttpStatus.OK);
    }

    @GetMapping("/get")
    public ResponseEntity<List<OrderDTO>> getAll(@RequestParam(required = false) List<OrderStatus> statuses) {
        return new ResponseEntity<List<OrderDTO>>(orderService.getAll(statuses), HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<List<OrderDTO>> search(
            @RequestParam(required = false) String searchTerm) {
        List<OrderDTO> orders = orderService.searchOrders(searchTerm);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/track/{trackingId}")
    public ResponseEntity<OrderDTO> trackOrder(@PathVariable String trackingId) {
        return new ResponseEntity<OrderDTO>(orderService.getOrderByTrackingId(trackingId), HttpStatus.OK);
    }

    @PostMapping("/create")
    public OrderDTO createOrder(@RequestBody OrderRecord order) {
        return orderService.createOrder(order);
    }

    @PutMapping("/{id}")
    public OrderDTO updateOrder(@PathVariable Long id, @RequestBody OrderRecord order) {
        order.setId(id);
        return orderService.updateOrder(order);
    }

    @PostMapping("/changeStatus/{id}")
    public OrderDTO changeStatus(@PathVariable Long id) {
        return orderService.changeStatus(id);
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }

}

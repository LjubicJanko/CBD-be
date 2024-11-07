package cbd.order_tracker.controller;

import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.Payment;
import cbd.order_tracker.model.dto.*;
import cbd.order_tracker.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        return new ResponseEntity<>(orderService.getOrderById(id), HttpStatus.OK);
    }

    @GetMapping("/get")
    public ResponseEntity<List<OrderDTO>> getAll(@RequestParam(required = false) List<OrderStatus> statuses) {
        return new ResponseEntity<>(orderService.getAll(statuses), HttpStatus.OK);
    }


    @GetMapping("/getPageable")
    public ResponseEntity<PageableResponse<OrderOverviewDto>> getAllPageable(
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = true) Integer page,
            @RequestParam(required = true) Integer perPage,
            @RequestParam(required = false) List<OrderExecutionStatus> executionStatuses) {

        // Use default filter if no executionStatuses are provided
        if (executionStatuses == null || executionStatuses.isEmpty()) {
            executionStatuses = List.of(OrderExecutionStatus.ACTIVE, OrderExecutionStatus.PAUSED);
        }

        PageableResponse<OrderOverviewDto> response = orderService.getAllPageable(statuses, executionStatuses, page, perPage);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @GetMapping("/search")
    public ResponseEntity<PageableResponse<OrderOverviewDto>> search(@RequestParam(required = false) String searchTerm, @RequestParam(required = true) Integer page, @RequestParam(required = true) Integer perPage) {
        return new ResponseEntity<PageableResponse<OrderOverviewDto>>(orderService.searchOrders(searchTerm, page, perPage), HttpStatus.OK);
    }

    @GetMapping("/track/{trackingId}")
    public ResponseEntity<OrderTrackingDTO> trackOrder(@PathVariable String trackingId) {
        return new ResponseEntity<OrderTrackingDTO>(orderService.getOrderByTrackingId(trackingId), HttpStatus.OK);
    }

    @PostMapping("/create")
    public OrderDTO createOrder(@RequestBody OrderRecord order) {
        System.out.println(order);
        return orderService.createOrder(order);
    }

    @PutMapping("/{id}")
    public OrderDTO updateOrder(@PathVariable Long id, @RequestBody OrderRecord order) {
        order.setId(id);
        return orderService.updateOrder(order);
    }

    @PutMapping("/changeExecutionStatus/{id}")
    public OrderDTO changeExecutionStatus(@PathVariable Long id, @RequestBody ChangeExecutionStatusRequestDto request) {
        return orderService.changeExecutionStatus(id, request.getExecutionStatus(), request.getNote());
    }

    @PutMapping("/pause/{id}")
    public OrderDTO pauseOrder(@PathVariable Long id, @RequestBody String pausingComment) {
        return orderService.pauseOrder(id, pausingComment);
    }

    @PutMapping("/reactivate/{id}")
    public OrderDTO reactivateOrder(@PathVariable Long id) {
        return orderService.reactivateOrder(id);
    }

    // Updated changeStatus to accept optional fields via request body
    @PostMapping("/changeStatus/{id}")
    public OrderDTO changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String closingComment = body.getOrDefault("closingComment", null);
        String postalCode = body.getOrDefault("postalCode", null);
        String postalService = body.getOrDefault("postalService", null);

        return orderService.changeStatus(id, closingComment, postalCode, postalService);
    }

    @PostMapping("/addPayment/{id}")
    public ResponseEntity<OrderDTO> addPayment(@PathVariable Long id, @RequestBody Payment payment) {
        return new ResponseEntity<>(orderService.addPayment(id, payment), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }
}

package cbd.order_tracker.controller;

import cbd.order_tracker.exceptions.OrderNotFoundException;
import cbd.order_tracker.model.*;
import cbd.order_tracker.model.dto.*;
import cbd.order_tracker.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@GetMapping("/get/{id}")
	public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
		return new ResponseEntity<>(orderService.getOrderById(id), HttpStatus.OK);
	}

	@GetMapping("/get")
	public ResponseEntity<List<OrderDTO>> getAll(@RequestParam(required = false) List<OrderStatus> statuses) {
		return new ResponseEntity<>(orderService.getAll(statuses), HttpStatus.OK);
	}

	@GetMapping("/fetchPageable")
	public ResponseEntity<PageableResponse<OrderOverviewDto>> fetchPageable(
			@RequestParam(required = false) String searchTerm,
			@RequestParam(required = false) List<OrderStatus> statuses,
			@RequestParam(required = false) List<OrderPriority> priorities,
			@RequestParam(required = false) String sortCriteria,
			@RequestParam(required = false) String sort,
			@RequestParam(required = true) Integer page,
			@RequestParam(required = true) Integer perPage,
			@RequestParam(required = false) List<OrderExecutionStatus> executionStatuses) {

		// Apply default filter for executionStatuses if not provided
		if (executionStatuses == null || executionStatuses.isEmpty()) {
			executionStatuses = List.of(OrderExecutionStatus.ACTIVE, OrderExecutionStatus.PAUSED);
		}

		// Delegate to the service layer with all parameters
		PageableResponse<OrderOverviewDto> response = orderService.fetchPageable(searchTerm, statuses, priorities, sortCriteria, sort, executionStatuses, page, perPage);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/payments/{id}")
	public List<Payment> getPayments(@PathVariable Long id) {
		return orderService.getPayments(id);
	}

	@GetMapping("/history/{id}")
	public List<OrderStatusHistoryDTO> getHistory(@PathVariable Long id) {
		return orderService.getHistory(id);
	}

	@GetMapping("/search")
	public ResponseEntity<PageableResponse<OrderOverviewDto>> search(@RequestParam(required = false) String searchTerm, @RequestParam(required = true) Integer page, @RequestParam(required = true) Integer perPage) {
		return new ResponseEntity<PageableResponse<OrderOverviewDto>>(orderService.searchOrders(searchTerm, page, perPage), HttpStatus.OK);
	}

	@GetMapping("/track/{trackingId}")
	public ResponseEntity<OrderTrackingDTO> trackOrder(@PathVariable String trackingId) {
		try {
			OrderTrackingDTO trackingDTO = orderService.getOrderByTrackingId(trackingId);
			return ResponseEntity.ok(trackingDTO); // Return 200 OK with the order tracking data
		} catch (OrderNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Return 404 with no body
		}
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
	public ResponseEntity<UpdatePaymentsResponse> addPayment(@PathVariable Long id, @RequestBody PaymentRequestDto payment) {
		return new ResponseEntity<>(orderService.addPayment(id, payment), HttpStatus.OK);
	}

	@PutMapping("/editPayment/{id}")
	public ResponseEntity<UpdatePaymentsResponse> editPayment(@PathVariable Long id, @RequestBody Payment payment) {
		return new ResponseEntity<>(orderService.editPayment(id, payment), HttpStatus.OK);
	}

	@PutMapping("/deletePayment/{id}")
	public ResponseEntity<UpdatePaymentsResponse> deletePayment(@PathVariable Long id, @RequestParam Long paymentId) {
		return new ResponseEntity<>(orderService.deletePayment(id, paymentId), HttpStatus.OK);
	}

	@DeleteMapping("/delete/{id}")
	public void deleteOrder(@PathVariable Long id) {
		orderService.deleteOrder(id);
	}
}

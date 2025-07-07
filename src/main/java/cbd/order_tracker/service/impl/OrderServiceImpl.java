package cbd.order_tracker.service.impl;

import cbd.order_tracker.exceptions.OrderNotFoundException;
import cbd.order_tracker.model.*;
import cbd.order_tracker.model.dto.*;
import cbd.order_tracker.repository.*;
import cbd.order_tracker.service.OrderService;
import cbd.order_tracker.util.OrderMapper;
import cbd.order_tracker.util.PaymentMapper;
import cbd.order_tracker.util.UserUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private OrderStatusHistoryRepository statusHistoryRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserUtil userUtil;

	@Override
	public OrderDTO createOrder(OrderRecord order) {
		OrderRecord newOrder = new OrderRecord(order);
		OrderRecord orderRecord = orderRepository.save(newOrder);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String username = auth.getName();
		User user = userRepository.findByUsernameWithRoles(username)
				.orElseThrow(() -> new RuntimeException("User not found"));

		return OrderMapper.toDto(orderRecord, new ArrayList<>(), user.getRoles());
	}

	@Override
	public OrderDTO updateOrder(OrderRecord order) {
		OrderRecord orderRecord = orderRepository.findById(order.getId())
				.orElseThrow(() -> new RuntimeException("Order not found"));
		orderRecord.setName(order.getName());
		orderRecord.setDescription(order.getDescription());
		orderRecord.setNote(order.getNote());
		orderRecord.setSalePrice(order.getSalePrice());
		orderRecord.setSalePriceWithTax(order.getSalePrice().multiply(BigDecimal.valueOf(1.2)));
		orderRecord.setAcquisitionCost(order.getAcquisitionCost());
		orderRecord.setPriority(order.getPriority());
		orderRecord.setPlannedEndingDate(order.getPlannedEndingDate());
		orderRecord.setLegalEntity(order.isLegalEntity());

		var history = getOrderStatusHistory(order.getId());
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String username = auth.getName();
		User user = userRepository.findByUsernameWithRoles(username)
				.orElseThrow(() -> new RuntimeException("User not found"));

		return OrderMapper.toDto(orderRepository.save(orderRecord), history, user.getRoles());
	}

	@Override
	public OrderDTO changeExecutionStatus(Long id, OrderExecutionStatus executionStatus, String note) {
		OrderRecord orderRecord = orderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Order not found"));
		orderRecord.setExecutionStatus(executionStatus);
		orderRecord.setPausingComment(note);
		orderRepository.save(orderRecord);

		List<OrderStatusHistory> history = statusHistoryRepository.findByOrderId(id);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String username = auth.getName();
		User user = userRepository.findByUsernameWithRoles(username)
				.orElseThrow(() -> new RuntimeException("User not found"));

		return OrderMapper.toDto(orderRecord, history, user.getRoles());
	}

	@Override
	public OrderDTO pauseOrder(Long id, String pausingComment) {
		OrderRecord orderRecord = orderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Order not found"));
		orderRecord.setExecutionStatus(OrderExecutionStatus.PAUSED);
		orderRecord.setPausingComment(pausingComment);
		orderRepository.save(orderRecord);

		List<OrderStatusHistory> history = statusHistoryRepository.findByOrderId(id);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String username = auth.getName();
		User user = userRepository.findByUsernameWithRoles(username)
				.orElseThrow(() -> new RuntimeException("User not found"));

		return OrderMapper.toDto(orderRecord, history, user.getRoles());
	}

	@Override
	public OrderDTO reactivateOrder(Long id) {
		OrderRecord orderRecord = orderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Order not found"));
		orderRecord.setExecutionStatus(OrderExecutionStatus.ACTIVE);
		orderRepository.save(orderRecord);

		List<OrderStatusHistory> history = statusHistoryRepository.findByOrderId(id);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String username = auth.getName();
		User user = userRepository.findByUsernameWithRoles(username)
				.orElseThrow(() -> new RuntimeException("User not found"));

		return OrderMapper.toDto(orderRecord, history, user.getRoles());
	}

	@Override
	public OrderDTO changeStatus(Long id, String closingComment, String postalCode, String postalService) {
		OrderRecord orderRecord = orderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Order not found"));
		try {
			String currentUser = UserUtil.getCurrentUserName();
			List<OrderStatusHistory> historyList = getOrderStatusHistory(id);
			var lastHistoryRecord = historyList.get(historyList.size() - 1);
			lastHistoryRecord.setClosingComment(closingComment);
			lastHistoryRecord.setUser(currentUser);
			orderRecord.setStatusHistory(historyList);
			orderRecord.nextStatus(postalCode, postalService);
			orderRepository.save(orderRecord);
		} catch (IllegalStateException error) {
			System.out.println(error.getMessage());
		}

		List<OrderStatusHistory> history = statusHistoryRepository.findByOrderId(id);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String username = auth.getName();
		User user = userRepository.findByUsernameWithRoles(username)
				.orElseThrow(() -> new RuntimeException("User not found"));

		return OrderMapper.toDto(orderRecord, history, user.getRoles());
	}

	@Transactional
	@Override
	public UpdatePaymentsResponse addPayment(Long id, PaymentRequestDto payment) {
		OrderRecord orderRecord = orderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Order not found"));
		payment.setOrder(orderRecord);
		orderRecord.addPayment(payment);
		orderRecord = orderRepository.save(orderRecord);
		System.out.println(orderRecord);

		List<PaymentDto> payments = orderRecord.getPayments().stream().map(PaymentMapper::toDto).toList();

		BigDecimal priceForCalculation = orderRecord.isLegalEntity() ? orderRecord.getSalePriceWithTax() : orderRecord.getSalePrice();
		BigDecimal amountLefToPay = priceForCalculation.subtract(orderRecord.getAmountPaid());
		return new UpdatePaymentsResponse(payments, orderRecord.getAmountPaid(), amountLefToPay);
	}

	@Transactional
	@Override
	public UpdatePaymentsResponse editPayment(Long orderId, Payment updatedPayment) {
		OrderRecord orderRecord = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found"));
		Payment existingPayment = orderRecord.getPayments().stream()
				.filter(payment -> payment.getId().equals(updatedPayment.getId()))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Payment not found"));

		var diff = existingPayment.getAmount().subtract(updatedPayment.getAmount());
		if (diff.compareTo(BigDecimal.ZERO) != 0) {
			var newAmountPaid = orderRecord.getAmountPaid().subtract(diff);
			orderRecord.setAmountPaid(newAmountPaid);
			orderRecord.setAmountLeftToPay(orderRecord.getSalePrice().subtract(newAmountPaid));
			orderRecord.setAmountLeftToPayWithTax(orderRecord.getSalePriceWithTax().subtract(newAmountPaid));
		}
		existingPayment.setPayer(updatedPayment.getPayer());
		existingPayment.setAmount(updatedPayment.getAmount());
		existingPayment.setPaymentDate(updatedPayment.getPaymentDate());
		existingPayment.setPaymentMethod(updatedPayment.getPaymentMethod());
		existingPayment.setNote(updatedPayment.getNote());
		orderRecord = orderRepository.save(orderRecord);

		List<PaymentDto> payments = orderRecord.getPayments().stream().map(PaymentMapper::toDto).toList();
		BigDecimal priceForCalculation = orderRecord.isLegalEntity() ? orderRecord.getSalePriceWithTax() : orderRecord.getSalePrice();
		BigDecimal amountLefToPay = priceForCalculation.subtract(orderRecord.getAmountPaid());
		return new UpdatePaymentsResponse(payments, orderRecord.getAmountPaid(), amountLefToPay);
	}

	@Transactional
	@Override
	public UpdatePaymentsResponse deletePayment(Long orderId, Long paymentId) {
		OrderRecord orderRecord = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found"));
		Payment paymentToDelete = orderRecord.getPayments().stream()
				.filter(payment -> payment.getId().equals(paymentId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Payment not found"));

		orderRecord.getPayments().remove(paymentToDelete);
		orderRecord.setAmountPaid(orderRecord.getAmountPaid().subtract(paymentToDelete.getAmount()));
		orderRecord.setAmountLeftToPay(orderRecord.getSalePrice().subtract(orderRecord.getAmountPaid()));
		orderRecord.setAmountLeftToPayWithTax(orderRecord.getSalePriceWithTax().subtract(orderRecord.getAmountPaid()));
		orderRecord = orderRepository.save(orderRecord);
		paymentRepository.delete(paymentToDelete);

		List<PaymentDto> payments = orderRecord.getPayments().stream().map(PaymentMapper::toDto).toList();
		BigDecimal priceForCalculation = orderRecord.isLegalEntity() ? orderRecord.getSalePriceWithTax() : orderRecord.getSalePrice();
		BigDecimal amountLefToPay = priceForCalculation.subtract(orderRecord.getAmountPaid());
		return new UpdatePaymentsResponse(payments, orderRecord.getAmountPaid(), amountLefToPay);

	}

	@Override
	public OrderDTO getOrderById(Long id) {
		OrderRecord orderRecord = orderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Order not found"));
//		List<OrderStatusHistory> history = statusHistoryRepository.findByOrderId(id);
		Set<Role> roles = userUtil.getCurrentUserRoles();

		return OrderMapper.toDto(orderRecord, new ArrayList<>(), roles);
	}

	@Transactional(readOnly = true)
	@Override
	public PageableResponse<OrderOverviewDto> fetchPageable(
			String searchTerm,
			List<OrderStatus> statuses,
			List<OrderPriority> priorities,
			String sortCriteria,
			String sort,
			List<OrderExecutionStatus> executionStatuses,
			Integer page,
			Integer perPage) {
		if (executionStatuses == null || executionStatuses.isEmpty()) {
			executionStatuses = List.of(OrderExecutionStatus.ACTIVE, OrderExecutionStatus.PAUSED);
		}
		var direction = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
		var sortProp = "creation-date".equals(sortCriteria) ? "creationTime" : "plannedEndingDate";
		Pageable pageRequest = PageRequest.of(page, perPage, Sort.by(direction, sortProp));
		Page<OrderOverviewDto> orderDtos = orderRepository.findOverviewBySearchAndFilters(
				searchTerm, statuses, priorities, executionStatuses, pageRequest);

		return new PageableResponse<>(page, perPage, orderDtos.getTotalPages(),
				orderDtos.getTotalElements(), orderDtos.getContent());
	}

	@Override
	public List<OrderDTO> getAll(List<OrderStatus> statuses) {
		Iterable<OrderRecord> orderRecords = (statuses != null && !statuses.isEmpty()) ?
				orderRepository.findByStatusIn(statuses) : orderRepository.findAll();

		List<OrderRecord> orderRecordList = StreamSupport.stream(orderRecords.spliterator(), false)
				.toList();

		return orderRecordList.stream()
				.map(orderRecord -> {
					List<OrderStatusHistory> history = statusHistoryRepository.findByOrderId(orderRecord.getId());
					Authentication auth = SecurityContextHolder.getContext().getAuthentication();
					String username = auth.getName();
					User user = userRepository.findByUsernameWithRoles(username)
							.orElseThrow(() -> new RuntimeException("User not found"));
					return OrderMapper.toDto(orderRecord, history, user.getRoles());
				})
				.collect(Collectors.toList());
	}

	@Override
	public OrderTrackingDTO getOrderByTrackingId(String trackingId) {
		OrderRecord orderRecord = orderRepository.findByTrackingId(trackingId)
				.orElseThrow(() -> new OrderNotFoundException("Order with tracking ID '" + trackingId + "' not found"));
		List<OrderStatusHistory> history = getOrderStatusHistory(orderRecord.getId());
		orderRecord.setStatusHistory(history);
		return OrderMapper.toOrderTrackingDTO(orderRecord);
	}

	@Override
	public void deleteOrder(Long id) {
		OrderRecord order = orderRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
		order.setDeleted(true);
		orderRepository.save(order);
	}

	@Override
	public List<OrderStatusHistory> getOrderStatusHistory(Long orderId) {
		return statusHistoryRepository.findByOrderId(orderId);
	}

	@Override
	public PageableResponse<OrderOverviewDto> searchOrders(String searchTerm, Integer page, Integer perPage) {
		Pageable pageRequest = PageRequest.of(page, perPage);
		Page<OrderRecord> orderRecords;

		if (searchTerm == null || searchTerm.isEmpty()) {
			orderRecords = orderRepository.findAll(pageRequest);
		} else {
			orderRecords = orderRepository.findByNameContainingOrDescriptionContaining(searchTerm, searchTerm, pageRequest);
		}

		var orderOverviewDtos = orderRecords.stream().map(OrderMapper::toOverviewDto).collect(Collectors.toList());
		return new PageableResponse<>(
				page, perPage, orderRecords.getTotalPages(),
				orderRecords.getTotalElements(), orderOverviewDtos);
	}

	@Override
	public List<Payment> getPayments(Long orderId) {
		return paymentRepository.findByOrderId(orderId);
	}

	@Override
	public List<OrderStatusHistoryDTO> getHistory(Long orderId) {
		var history = statusHistoryRepository.findByOrderId(orderId);

		return OrderMapper.mapStatusHistory(history);
	}
}

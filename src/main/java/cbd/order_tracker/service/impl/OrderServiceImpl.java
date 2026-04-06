package cbd.order_tracker.service.impl;

import cbd.order_tracker.exceptions.OrderNotFoundException;
import cbd.order_tracker.model.*;
import cbd.order_tracker.model.dto.*;
import cbd.order_tracker.model.dto.request.CombineExtensionsReqDto;
import cbd.order_tracker.model.dto.request.EditShipmentInfoDto;
import cbd.order_tracker.model.dto.request.OrderExtensionReqDto;
import cbd.order_tracker.model.dto.response.OrderExtensionDto;
import cbd.order_tracker.repository.*;
import cbd.order_tracker.service.OrderService;
import cbd.order_tracker.util.OrderExtensionMapper;
import cbd.order_tracker.util.OrderMapper;
import cbd.order_tracker.util.PaymentMapper;
import cbd.order_tracker.util.UserUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final UserUtil userUtil;
	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;
	private final OrderStatusHistoryRepository statusHistoryRepository;

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
	public OrderExtensionDto createExtension(OrderExtensionReqDto orderExtensionReqDto) {
		OrderRecord newOrder = new OrderRecord(orderExtensionReqDto);
		OrderRecord orderRecord = orderRepository.save(newOrder);
		return OrderExtensionMapper.toDto(orderRecord);
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

	@Transactional
	@Override
	public OrderDTO changeExecutionStatus(Long id, OrderExecutionStatus executionStatus, String note) {
		OrderRecord orderRecord = orderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Order not found"));
		orderRecord.setExecutionStatus(executionStatus);
		orderRecord.setPausingComment(note);
		orderRecord.addExecutionStatusHistory(executionStatus, note);
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
		orderRecord.addExecutionStatusHistory(OrderExecutionStatus.PAUSED, pausingComment);
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
		orderRecord.addExecutionStatusHistory(OrderExecutionStatus.ACTIVE, null);
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
		Pageable pageRequest = PageRequest.of(
				page, perPage, Sort.by(direction, sortProp).and(Sort.by("id"))
		);
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
				.or(() -> orderRepository.findByAliasId(trackingId))
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

	@Override
	public OrderExtensionDto editExtension(String trackingId, OrderExtensionReqDto dto) {
		validateExtensionRequest(dto);

		OrderRecord orderRecord = orderRepository.findByTrackingId(trackingId)
				.or(() -> orderRepository.findByAliasId(trackingId))
				.orElseThrow(() -> new OrderNotFoundException("Order with tracking ID '" + trackingId + "' not found"));
		orderRecord.setName(dto.getName());
		orderRecord.setDescription(dto.getDescription());
		ContactInfo contactInfo = dto.getContactInfo();
		orderRecord.setContactInfo(contactInfo);
		orderRecord = orderRepository.save(orderRecord);
		return OrderExtensionMapper.toDto(orderRecord);
	}

	private void validateExtensionRequest(OrderExtensionReqDto dto) {
		if (dto.getName() == null || dto.getName().isBlank()) {
			throw new IllegalArgumentException("Name is required");
		}
		if (dto.getDescription() == null || dto.getDescription().isBlank()) {
			throw new IllegalArgumentException("Description is required");
		}
		ContactInfo ci = dto.getContactInfo();
		if (ci == null) {
			throw new IllegalArgumentException("Contact info is required");
		}
		if (ci.getFullName() == null || ci.getFullName().isBlank()) {
			throw new IllegalArgumentException("Full name is required");
		}
		if (ci.getPhoneNumber() == null || !ci.getPhoneNumber().matches("^[0-9+\\s-]{6,20}$")) {
			throw new IllegalArgumentException("Phone number is required and must match format: digits, +, spaces, or dashes (6-20 chars)");
		}
		if (ci.getZipCode() == null || !ci.getZipCode().matches("^\\d{4,6}$")) {
			throw new IllegalArgumentException("Zip code is required and must be 4-6 digits");
		}
		if (ci.getCity() == null || ci.getCity().isBlank()) {
			throw new IllegalArgumentException("City is required");
		}
		if (ci.getAddress() == null || ci.getAddress().isBlank()) {
			throw new IllegalArgumentException("Address is required");
		}
	}

	@Override
	public OrderExtensionDto editContactInfo(Long id, ContactInfo contactInfo) {
		OrderRecord orderRecord = orderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Order not found"));
		orderRecord.setContactInfo(contactInfo);
		orderRecord = orderRepository.save(orderRecord);
		return OrderExtensionMapper.toDto(orderRecord);
	}

	@Transactional
	@Override
	public OrderDTO editShipmentInfo(Long id, EditShipmentInfoDto dto) {
		OrderRecord orderRecord = orderRepository.findById(id)
				.orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

		if (orderRecord.getStatus() != OrderStatus.SHIPPED) {
			throw new IllegalStateException("Order must be in SHIPPED status to edit shipment info");
		}

		java.util.Set<String> validServices = java.util.Set.of("d", "city", "aks", "post", "bex");
		if (dto.getPostalService() == null || !validServices.contains(dto.getPostalService())) {
			throw new IllegalArgumentException("postalService must be one of: d, city, aks, post, bex");
		}
		if (dto.getPostalCode() == null || dto.getPostalCode().isBlank()) {
			throw new IllegalArgumentException("postalCode must not be empty");
		}

		orderRecord.setPostalService(dto.getPostalService());
		orderRecord.setPostalCode(dto.getPostalCode());

		// Update the SHIPPED status history entry as well
		List<OrderStatusHistory> history = statusHistoryRepository.findByOrderId(id);
		history.stream()
				.filter(h -> h.getStatus() == OrderStatus.SHIPPED)
				.findFirst()
				.ifPresent(h -> {
					h.setPostalService(dto.getPostalService());
					h.setPostalCode(dto.getPostalCode());
				});

		orderRepository.save(orderRecord);

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String username = auth.getName();
		User user = userRepository.findByUsernameWithRoles(username)
				.orElseThrow(() -> new RuntimeException("User not found"));

		return OrderMapper.toDto(orderRecord, history, user.getRoles());
	}

	@Transactional
	@Override
	public OrderExtensionDto combineExtensions(CombineExtensionsReqDto dto) {
		List<Long> ids = dto.getExtensionIds();
		if (ids == null || ids.size() < 2) {
			throw new IllegalArgumentException("At least 2 extension IDs are required");
		}

		Long mainId = ids.get(0);
		OrderRecord mainOrder = orderRepository.findById(mainId)
				.orElseThrow(() -> new OrderNotFoundException("Extension not found with id: " + mainId));

		if (!Boolean.TRUE.equals(mainOrder.getExtension())) {
			throw new IllegalArgumentException("Order with id " + mainId + " is not an extension");
		}

		List<String> aliasIds = new ArrayList<>(mainOrder.getAliasIds());

		for (int i = 1; i < ids.size(); i++) {
			Long otherId = ids.get(i);
			OrderRecord otherOrder = orderRepository.findById(otherId)
					.orElseThrow(() -> new OrderNotFoundException("Extension not found with id: " + otherId));

			if (!Boolean.TRUE.equals(otherOrder.getExtension())) {
				throw new IllegalArgumentException("Order with id " + otherId + " is not an extension");
			}

			aliasIds.add(otherOrder.getTrackingId());
			aliasIds.addAll(otherOrder.getAliasIds());

			otherOrder.setDeleted(true);
			orderRepository.save(otherOrder);
		}

		mainOrder.setName(dto.getName());
		mainOrder.setDescription(dto.getDescription());
		mainOrder.setContactInfo(dto.getContactInfo());
		mainOrder.setAliasIds(aliasIds);

		orderRepository.save(mainOrder);
		return OrderExtensionMapper.toDto(mainOrder);
	}
}

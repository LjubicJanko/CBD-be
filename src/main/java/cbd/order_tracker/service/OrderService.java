package cbd.order_tracker.service;

import cbd.order_tracker.model.*;
import cbd.order_tracker.model.dto.*;
import cbd.order_tracker.model.dto.request.OrderExtensionReqDto;
import cbd.order_tracker.model.dto.response.OrderExtensionDto;

import java.util.List;

public interface OrderService {

    OrderDTO createOrder(OrderRecord order);

    OrderExtensionDto createExtension(OrderExtensionReqDto order);

    OrderDTO updateOrder(OrderRecord order);

    OrderDTO changeExecutionStatus(Long id, OrderExecutionStatus executionStatus, String note);

    OrderDTO pauseOrder(Long id, String pausingComment);

    OrderDTO reactivateOrder(Long id);

    OrderDTO changeStatus(Long id, String closingComment, String postalCode, String postalService);

    UpdatePaymentsResponse addPayment(Long id, PaymentRequestDto payment);

    UpdatePaymentsResponse editPayment(Long orderId, Payment updatedPayment);

    UpdatePaymentsResponse deletePayment(Long orderId, Long paymentId);

    OrderDTO getOrderById(Long id);

    PageableResponse<OrderOverviewDto> fetchPageable(
            String searchTerm,
            List<OrderStatus> statuses,
            List<OrderPriority> priorities,
            String sortCriteria,
            String sort,
            List<OrderExecutionStatus> executionStatuses,
            Integer page,
            Integer perPage
    );

    List<OrderDTO> getAll(List<OrderStatus> statuses);

    OrderTrackingDTO getOrderByTrackingId(String trackingId);

    void deleteOrder(Long id);

    List<OrderStatusHistory> getOrderStatusHistory(Long orderId);

    PageableResponse<OrderOverviewDto> searchOrders(String searchTerm, Integer page, Integer perPage);

    List<Payment> getPayments(Long orderId);

    List<OrderStatusHistoryDTO> getHistory(Long orderId);

    OrderExtensionDto editContactInfo(Long id, ContactInfo contactInfo);
}

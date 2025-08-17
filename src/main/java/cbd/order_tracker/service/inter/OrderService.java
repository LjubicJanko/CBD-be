package cbd.order_tracker.service.inter;

import cbd.order_tracker.model.*;
import cbd.order_tracker.model.dto.*;

import java.util.List;

/**
 * Service interface for managing orders within the platform.
 * <p>
 * Provides methods for creating, updating, pausing, reactivating, and deleting orders,
 * as well as managing payments, order status changes, and retrieving order history.
 * </p>
 */
public interface OrderService {

    /**
     * Creates a new order for an unspecified company.
     *
     * @param order the {@link OrderRecord} containing order details
     * @return the created {@link OrderDTO}
     */
    OrderDTO createOrder(OrderRecord order);

    /**
     * Creates a new order for a specific company.
     *
     * @param companyId the ID of the company
     * @param order the {@link OrderRecord} containing order details
     * @return the created {@link OrderDTO}
     */
    OrderDTO createOrder(Long companyId, OrderRecord order);

    /**
     * Updates an existing order.
     *
     * @param order the {@link OrderRecord} containing updated information
     * @return the updated {@link OrderDTO}
     */
    OrderDTO updateOrder(OrderRecord order);

    /**
     * Changes the execution status of an order.
     *
     * @param id the order ID
     * @param executionStatus the new {@link OrderExecutionStatus}
     * @param note optional note regarding the change
     * @return the updated {@link OrderDTO}
     */
    OrderDTO changeExecutionStatus(Long id, OrderExecutionStatus executionStatus, String note);

    /**
     * Pauses an order.
     *
     * @param id the order ID
     * @param pausingComment comment explaining why the order is paused
     * @return the updated {@link OrderDTO}
     */
    OrderDTO pauseOrder(Long id, String pausingComment);

    /**
     * Reactivates a paused order.
     *
     * @param id the order ID
     * @return the updated {@link OrderDTO}
     */
    OrderDTO reactivateOrder(Long id);

    /**
     * Changes the status of an order, e.g., marking it as completed or closed.
     *
     * @param id the order ID
     * @param closingComment comment explaining the closing
     * @param postalCode postal code for shipment
     * @param postalService postal service used
     * @return the updated {@link OrderDTO}
     */
    OrderDTO changeStatus(Long id, String closingComment, String postalCode, String postalService);

    /**
     * Adds a payment to an order.
     *
     * @param id the order ID
     * @param payment the {@link PaymentRequestDto} containing payment information
     * @return {@link UpdatePaymentsResponse} reflecting updated payments
     */
    UpdatePaymentsResponse addPayment(Long id, PaymentRequestDto payment);

    /**
     * Edits an existing payment of an order.
     *
     * @param orderId the order ID
     * @param updatedPayment the updated {@link Payment} information
     * @return {@link UpdatePaymentsResponse} reflecting updated payments
     */
    UpdatePaymentsResponse editPayment(Long orderId, Payment updatedPayment);

    /**
     * Deletes a payment from an order.
     *
     * @param orderId the order ID
     * @param paymentId the payment ID to remove
     * @return {@link UpdatePaymentsResponse} reflecting updated payments
     */
    UpdatePaymentsResponse deletePayment(Long orderId, Long paymentId);

    /**
     * Retrieves an order by its ID.
     *
     * @param id the order ID
     * @return the {@link OrderDTO}
     */
    OrderDTO getOrderById(Long id);

    /**
     * Retrieves a pageable list of orders, optionally filtered and sorted.
     *
     * @param searchTerm search keyword
     * @param statuses list of {@link OrderStatus} to filter
     * @param priorities list of {@link OrderPriority} to filter
     * @param sortCriteria field to sort by
     * @param sort direction ("asc" or "desc")
     * @param executionStatuses list of {@link OrderExecutionStatus} to filter
     * @param page page number
     * @param perPage items per page
     * @return {@link PageableResponse} containing {@link OrderOverviewDto}
     */
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

    /**
     * Retrieves a pageable list of orders for a specific company, optionally filtered and sorted.
     *
     * @param companyId the company ID
     * @param searchTerm search keyword
     * @param statuses list of {@link OrderStatus} to filter
     * @param priorities list of {@link OrderPriority} to filter
     * @param sortCriteria field to sort by
     * @param sort direction ("asc" or "desc")
     * @param executionStatuses list of {@link OrderExecutionStatus} to filter
     * @param page page number
     * @param perPage items per page
     * @return {@link PageableResponse} containing {@link OrderOverviewDto}
     */
    PageableResponse<OrderOverviewDto> fetchPageable(
            Long companyId,
            String searchTerm,
            List<OrderStatus> statuses,
            List<OrderPriority> priorities,
            String sortCriteria,
            String sort,
            List<OrderExecutionStatus> executionStatuses,
            Integer page,
            Integer perPage
    );

    /**
     * Retrieves all orders filtered by their statuses.
     *
     * @param statuses list of {@link OrderStatus} to filter
     * @return list of {@link OrderDTO}
     */
    List<OrderDTO> getAll(List<OrderStatus> statuses);

    /**
     * Retrieves an order by its tracking ID.
     *
     * @param trackingId the tracking ID
     * @return the {@link OrderTrackingDTO}
     */
    OrderTrackingDTO getOrderByTrackingId(String trackingId);

    /**
     * Deletes an order.
     *
     * @param id the order ID
     */
    void deleteOrder(Long id);

    /**
     * Retrieves the status history of a given order.
     *
     * @param orderId the order ID
     * @return list of {@link OrderStatusHistory}
     */
    List<OrderStatusHistory> getOrderStatusHistory(Long orderId);

    /**
     * Searches orders by keyword with pagination.
     *
     * @param searchTerm search keyword
     * @param page page number
     * @param perPage items per page
     * @return {@link PageableResponse} containing {@link OrderOverviewDto}
     */
    PageableResponse<OrderOverviewDto> searchOrders(String searchTerm, Integer page, Integer perPage);

    /**
     * Retrieves all payments for a given order.
     *
     * @param orderId the order ID
     * @return list of {@link Payment}
     */
    List<Payment> getPayments(Long orderId);

    /**
     * Retrieves the order status history as DTOs for a given order.
     *
     * @param orderId the order ID
     * @return list of {@link OrderStatusHistoryDTO}
     */
    List<OrderStatusHistoryDTO> getHistory(Long orderId);
}

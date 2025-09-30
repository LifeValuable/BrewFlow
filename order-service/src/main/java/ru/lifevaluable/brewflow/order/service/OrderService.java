package ru.lifevaluable.brewflow.order.service;

import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.lifevaluable.brewflow.order.dto.OrderResponse;
import ru.lifevaluable.brewflow.order.dto.OrdersHistoryResponse;
import ru.lifevaluable.brewflow.order.dto.UserData;
import ru.lifevaluable.brewflow.order.entity.*;
import ru.lifevaluable.brewflow.order.exception.*;
import ru.lifevaluable.brewflow.order.mapper.OrderMapper;
import ru.lifevaluable.brewflow.order.repository.CartItemRepository;
import ru.lifevaluable.brewflow.order.repository.OrderRepository;
import ru.lifevaluable.brewflow.order.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrderFromCart(UUID userId, UserData userData) {
        log.debug("Create order from cart: userId={}", userId);
        validateNotNull(userId, "User id");
        validateNotNull(userData, "User data");
        if (!userId.equals(userData.id())) {
            throw new IllegalArgumentException(
                    String.format("User id %s is not equal userData.id %s",
                            userId.toString(), userData.id().toString())
            );
        }

        List<CartItem> cartItems = cartRepository.findByIdUserIdWithProducts(userId);
        if (cartItems.isEmpty())
            throw new EmptyCartException(userId);

        List<OrderItem> orderItems = orderMapper.cartItemToOrderItem(cartItems);

        Order order = new Order();
        order.setItems(orderItems);
        order.setTotalPrice(calculateTotalPrice(orderItems));
        order.setUserId(userId);
        order.setUserFirstName(userData.firstName());
        order.setUserLastName(userData.lastName());
        order.setUserEmail(userData.email());
        order.setStatus(OrderStatus.CREATED);

        try {
            for (OrderItem orderItem : orderItems) {
                orderItem.setOrder(order);
                Product product = orderItem.getProduct();
                int reduced = productRepository.reduceStockQuantity(product.getId(), orderItem.getQuantity());
                if (reduced == 0)
                    throw new InsufficientStockException(product.getId(), orderItem.getQuantity(), product.getStockQuantity());
            }
        }
        catch (OptimisticLockException ex) {
            throw new OrderCreationFailedException("Order cannot be processed due to inventory changes. Please try again.");
        }

        //отправить запрос об оплате

        Order savedOrder = orderRepository.save(order);
        log.info("Created order for user: userId={}, orderId={}", userId, savedOrder.getId());
        int deletedItems = cartRepository.removeByIdUserId(userId);
        log.info("Cleaned user cart: userId={}, deletedItems={}", userId, deletedItems);
        return orderMapper.toDTO(savedOrder);
    }

    public OrderResponse getOrderDetails(UUID orderId, UUID userId) {
        log.debug("Get order details: userId={}, orderId={}", orderId, userId);
        validateNotNull(orderId, "Order id");
        validateNotNull(userId, "User id");
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        // пользователю не обязательно знать, что запрошен чужой заказ
        if (!order.getUserId().equals(userId))
            throw new OrderNotFoundException(orderId);
        log.info("Get order details: userId={}, orderId={}", orderId, userId);
        return orderMapper.toDTO(order);
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, UUID userId, OrderStatus currentStatus, OrderStatus newStatus) {
        log.debug("Update order status: userId={}, orderId={}, currentStatus={}, newStatus={}", userId, orderId, currentStatus, newStatus);
        validateNotNull(orderId, "Order id");
        validateNotNull(userId, "User id");
        validateNotNull(currentStatus, "Current status");
        validateNotNull(newStatus, "New status");
        if (newStatus.compareTo(currentStatus) <= 0)
            throw new InvalidOrderStatusTransitionException(orderId, currentStatus, newStatus);

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        // пользователю не обязательно знать, что запрошен чужой заказ
        if (!order.getUserId().equals(userId))
            throw new OrderNotFoundException(orderId);

        order.setStatus(newStatus);
        log.info("Update order status: userId={}, orderId={}, currentStatus={}, newStatus={}", userId, orderId, currentStatus, newStatus);
    }

    public OrdersHistoryResponse getOrdersHistory(UUID userId) {
        log.debug("Get orders history: userId={}", userId);
        validateNotNull(userId, "User id");
        return orderMapper.toHistoryDTO(orderRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }


    private void validateNotNull(Object obj, String name) {
        if (obj == null)
            throw new IllegalArgumentException(String.format("%s can not be null", name));
    }

    private BigDecimal calculateTotalPrice(List<OrderItem> items) {
        MathContext mc = new MathContext(10, RoundingMode.UNNECESSARY);
        BigDecimal zero = new BigDecimal(0, mc).setScale(2, RoundingMode.UNNECESSARY);
        return items.stream()
                .map(item ->
                    item.getPriceAtTime()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                )
                .reduce(zero, BigDecimal::add);
    }
}

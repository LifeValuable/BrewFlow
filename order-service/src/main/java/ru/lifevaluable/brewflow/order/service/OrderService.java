package ru.lifevaluable.brewflow.order.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.lifevaluable.brewflow.order.dto.OrderResponse;
import ru.lifevaluable.brewflow.order.dto.OrdersHistoryResponse;
import ru.lifevaluable.brewflow.order.dto.UserData;
import ru.lifevaluable.brewflow.order.entity.CartItem;
import ru.lifevaluable.brewflow.order.entity.Order;
import ru.lifevaluable.brewflow.order.entity.OrderItem;
import ru.lifevaluable.brewflow.order.entity.OrderStatus;
import ru.lifevaluable.brewflow.order.entity.Product;
import ru.lifevaluable.brewflow.order.event.OrderCreatedEvent;
import ru.lifevaluable.brewflow.order.exception.*;
import ru.lifevaluable.brewflow.order.mapper.OrderMapper;
import ru.lifevaluable.brewflow.order.repository.CartItemRepository;
import ru.lifevaluable.brewflow.order.repository.OrderRepository;
import ru.lifevaluable.brewflow.order.repository.ProductRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartRepository;
    private final OrderMapper orderMapper;
    private final EventPublisher eventPublisher;
    private final CacheManager cacheManager;

    @Transactional
    public OrderResponse createOrderFromCart(UUID userId, UserData userData) {
        log.debug("Create order from cart userId={}", userId);
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

        List<UUID> productIds = cartItems.stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();

        List<Product> lockedProducts = productRepository.findByIdInWithLockOrdered(productIds);
        Map<UUID, Product> productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProduct().getId());
            if (product == null) {
                throw new ProductNotFoundException(cartItem.getProduct().getId());
            }
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        product.getId(),
                        cartItem.getQuantity(),
                        product.getStockQuantity()
                );
            }
        }

        List<OrderItem> orderItems = orderMapper.cartItemToOrderItem(cartItems);

        Order order = new Order();
        order.setItems(orderItems);
        order.setTotalPrice(calculateTotalPrice(orderItems));
        order.setUserId(userId);
        order.setUserFirstName(userData.firstName());
        order.setUserLastName(userData.lastName());
        order.setUserEmail(userData.email());
        order.setStatus(OrderStatus.RESERVED);

        evictAllProductsCache();

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(order);
            Product product = productMap.get(orderItem.getProduct().getId());
            product.setStockQuantity(product.getStockQuantity() - orderItem.getQuantity());
            productRepository.save(product);
            evictProductCache(product.getId());
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Created order for user: userId={}, orderId={}", userId, savedOrder.getId());
        List<OrderCreatedEvent.OrderItemEvent> eventItems = savedOrder.getItems().stream()
                .map(item -> new OrderCreatedEvent.OrderItemEvent(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPriceAtTime()
                ))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                userId,
                userData.email(),
                userData.firstName(),
                userData.lastName(),
                savedOrder.getTotalPrice(),
                eventItems,
                LocalDateTime.now()
        );
        eventPublisher.publishOrderCreated(event);

        int deletedItems = cartRepository.removeByIdUserId(userId);
        log.info("Cleaned user cart: userId={}, deletedItems={}", userId, deletedItems);
        return orderMapper.toDTO(savedOrder);
    }

    public OrderResponse getOrderDetails(UUID orderId, UUID userId) {
        log.debug("Get order details: userId={}, orderId={}", orderId, userId);
        validateNotNull(orderId, "Order id");
        validateNotNull(userId, "User id");
        Order order = orderRepository.findByIdWithItems(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
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

    @Transactional
    public void cancelOrder(UUID orderId, UUID userId) {
        log.debug("Cancelling order: orderId={}, userId={}", orderId, userId);

        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Order is already cancelled: orderId={}, userId={}", orderId, userId);
            return;
        }

        evictAllProductsCache();

        List<UUID> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .toList();

        List<Product> lockedProducts = productRepository.findByIdInWithLockOrdered(productIds);
        Map<UUID, Product> productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        order.setStatus(OrderStatus.CANCELLED);

        for (OrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
            evictProductCache(product.getId());

            log.debug("Returned {} units of {} to stock. New stock = {}",
                    item.getQuantity(), product.getName(), product.getStockQuantity());
        }

        log.info("Order is cancelled orderId={}, userId={}", orderId, userId);
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

    @CacheEvict(value = "products", key = "#productId")
    private void evictProductCache(UUID productId) {
        log.debug("Evicted product from cache: {}", productId);
    }

    @CacheEvict(value = "allProducts")
    private void evictAllProductsCache() {
        log.debug("Cleared all products cache");
    }
}

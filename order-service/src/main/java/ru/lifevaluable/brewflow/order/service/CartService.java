package ru.lifevaluable.brewflow.order.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import ru.lifevaluable.brewflow.order.dto.CartItemResponse;
import ru.lifevaluable.brewflow.order.dto.CartResponse;
import ru.lifevaluable.brewflow.order.entity.CartItem;
import ru.lifevaluable.brewflow.order.entity.CartItemKey;
import ru.lifevaluable.brewflow.order.entity.Product;
import ru.lifevaluable.brewflow.order.exception.InsufficientStockException;
import ru.lifevaluable.brewflow.order.exception.ProductNotFoundException;
import ru.lifevaluable.brewflow.order.mapper.CartMapper;
import ru.lifevaluable.brewflow.order.repository.CartItemRepository;
import ru.lifevaluable.brewflow.order.repository.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    public CartResponse getUserCart(UUID userId) {
        log.debug("Get user cart: userId={}", userId);
        validateUserId(userId);
        List<CartItem> items = cartItemRepository.findByIdUserIdWithProducts(userId);
        log.debug("Got cart with {} items: userId={}", items.size(), userId);
        return cartMapper.toDTO(items);
    }

    @Retryable(
            retryFor = {ConstraintViolationException.class},
            maxAttempts = 2
    )
    @Transactional
    public CartItemResponse addItemToCart(UUID userId, UUID productId, int quantity) {
        log.debug("Add item to cart: userId={}, productId={}, quantity={}", userId, productId, quantity);
        validateUserIdAndProductId(userId, productId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity can not be smaller than 0");
        }

        Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        if (product.getStockQuantity() < quantity)
            throw new InsufficientStockException(productId, quantity, product.getStockQuantity());

        Optional<CartItem> cartItem = cartItemRepository.findByIdWithLock(userId, productId);
        CartItem item;
        if (cartItem.isEmpty()) {
            log.debug("Creating new cart item: userId={}, productId={}", userId, productId);
            CartItemKey cartItemKey = new CartItemKey();
            cartItemKey.setProductId(productId);
            cartItemKey.setUserId(userId);

            item = new CartItem();
            item.setId(cartItemKey);
            item.setQuantity(quantity);
            item.setProduct(product);
        }
        else {
            log.debug("Updating existing cart item: userId={}, productId={}, currentQuantity={}",
                    userId, productId, cartItem.get().getQuantity());
            item = cartItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        }

        cartItemRepository.save(item);
        log.info("Successfully added item to cart: userId={}, productId={}, quantity={}", userId, productId, quantity);
        return cartMapper.toDTO(item);
    }

    @Transactional
    public void removeItemFromCart(UUID userId, UUID productId) {
        log.debug("Remove item from cart: userId={}, productId={}", userId, productId);
        validateUserIdAndProductId(userId, productId);
        cartItemRepository.removeByIdUserIdAndIdProductId(userId, productId);
        log.info("Removed item from cart: userId={}, productId={}", userId, productId);
    }

    @Transactional
    public void clearCart(UUID userId) {
        log.debug("Clearing user cart: userId={}", userId);
        validateUserId(userId);
        int deletedItems = cartItemRepository.removeByIdUserId(userId);
        log.info("Cleaned user cart: userId={}, deletedItems={}", userId, deletedItems);
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id can not be null");
        }
    }

    private void validateUserIdAndProductId(UUID userId, UUID productId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id can not be null");
        }
        else if (productId == null) {
            throw new IllegalArgumentException("Product id can not be null");
        }
    }
}

package ru.lifevaluable.brewflow.order.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class CartService {
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    public CartResponse getUserCart(UUID userId) {
        validateUserId(userId);
        List<CartItem> items = cartItemRepository.findByIdUserIdWithProducts(userId);
        return cartMapper.toDTO(items);
    }

    @Transactional
    public CartItemResponse addItemToCart(UUID userId, UUID productId, int quantity) {
        validateUserIdAndProductId(userId, productId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity can not be smaller than 0");
        }

        Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        if (product.getStockQuantity() < quantity)
            throw new InsufficientStockException(productId, quantity, product.getStockQuantity());

        CartItemKey cartItemKey = new CartItemKey();
        cartItemKey.setProductId(productId);
        cartItemKey.setUserId(userId);

        Optional<CartItem> cartItem = cartItemRepository.findById(cartItemKey);
        CartItem item;
        if (cartItem.isEmpty()) {
            item = new CartItem();
            item.setId(cartItemKey);
            item.setQuantity(0);
            item.setProduct(product);
        }
        else {
            item = cartItem.get();
        }

        item.setQuantity(item.getQuantity() + quantity);
        cartItemRepository.save(item);
        return cartMapper.toDTO(item);
    }

    @Transactional
    public void removeItemFromCart(UUID userId, UUID productId) {
        validateUserIdAndProductId(userId, productId);
        cartItemRepository.removeByIdUserIdAndIdProductId(userId, productId);
    }

    @Transactional
    public void clearCart(UUID userId) {
        validateUserId(userId);
        cartItemRepository.removeByIdUserId(userId);
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

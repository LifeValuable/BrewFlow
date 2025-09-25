package ru.lifevaluable.brewflow.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import ru.lifevaluable.brewflow.order.dto.CartItemResponse;
import ru.lifevaluable.brewflow.order.dto.CartResponse;
import ru.lifevaluable.brewflow.order.entity.CartItem;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CartMapper {
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productPrice", source = "product.price")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "totalPrice", source = ".", qualifiedByName = "calculateTotalItemPrice")
    CartItemResponse toDTO(CartItem cartItem);

    @Mapping(target = "items", source = ".")
    @Mapping(target = "totalPrice", source = ".", qualifiedByName = "calculateTotalCartPrice")
    default CartResponse toDTO(List<CartItem> items) {
        List<CartItemResponse> responses = items.stream()
                .map(this::toDTO)
                .toList();

        return new CartResponse(responses, calculateTotalCartPrice(responses));
    }

    @Named("calculateTotalItemPrice")
    default BigDecimal calculateTotalItemPrice(CartItem cartItem) {
        BigDecimal price = cartItem.getProduct().getPrice();
        return price.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
    }

    default BigDecimal calculateTotalCartPrice(List<CartItemResponse> items) {
        MathContext mc = new MathContext(10, RoundingMode.UNNECESSARY);
        BigDecimal zero = new BigDecimal(0, mc).setScale(10, RoundingMode.UNNECESSARY);

        return items.stream()
                .map(CartItemResponse::totalPrice)
                .reduce(zero, BigDecimal::add);
    }
}

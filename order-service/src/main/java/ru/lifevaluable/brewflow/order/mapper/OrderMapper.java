package ru.lifevaluable.brewflow.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import ru.lifevaluable.brewflow.order.dto.OrderHistoryResponse;
import ru.lifevaluable.brewflow.order.dto.OrderItemResponse;
import ru.lifevaluable.brewflow.order.dto.OrderResponse;
import ru.lifevaluable.brewflow.order.dto.OrdersHistoryResponse;
import ru.lifevaluable.brewflow.order.entity.CartItem;
import ru.lifevaluable.brewflow.order.entity.Order;
import ru.lifevaluable.brewflow.order.entity.OrderItem;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "totalPrice", source = ".", qualifiedByName = "calculateItemPrice")
    OrderItemResponse toDTO(OrderItem orderItem);

    OrderResponse toDTO(Order order);

    @Mapping(target = "itemsCount", expression = "java(order.getItems().size())")
    OrderHistoryResponse toHistoryDTO(Order order);

    default OrdersHistoryResponse toHistoryDTO(List<Order> orders) {
        return new OrdersHistoryResponse(
                orders.stream()
                        .map(this::toHistoryDTO)
                        .toList()
        );
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "priceAtTime", source = "product.price")
    OrderItem cartItemToOrderItem(CartItem item);

    List<OrderItem> cartItemToOrderItem(List<CartItem> items);

    @Named("calculateItemPrice")
    default BigDecimal calculateItemPrice(OrderItem orderItem) {
        BigDecimal quantity = BigDecimal.valueOf(orderItem.getQuantity());
        return orderItem.getPriceAtTime().multiply(quantity);
    }
}

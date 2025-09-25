package ru.lifevaluable.brewflow.order.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Entity
@Data
@Table(name = "cart_items")
public class CartItem {
    @EmbeddedId
    private CartItemKey id;

    @Min(0)
    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    private Product product;
}

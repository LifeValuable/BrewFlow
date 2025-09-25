package ru.lifevaluable.brewflow.order.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
public class CartItemKey implements Serializable {
    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID productId;
}

package ru.lifevaluable.brewflow.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.lifevaluable.brewflow.order.entity.Order;
import ru.lifevaluable.brewflow.order.entity.OrderStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE Order o SET o.status = :newStatus WHERE o.id = :orderId AND o.status = :currentStatus")
    int updateOrderStatus(UUID orderId, OrderStatus currentStatus, OrderStatus newStatus);
}

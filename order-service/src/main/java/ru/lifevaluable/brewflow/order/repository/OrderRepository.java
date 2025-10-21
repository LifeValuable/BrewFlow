package ru.lifevaluable.brewflow.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import ru.lifevaluable.brewflow.order.entity.Order;
import ru.lifevaluable.brewflow.order.entity.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE Order o SET o.status = :newStatus WHERE o.id = :orderId AND o.status = :currentStatus")
    int updateOrderStatus(UUID orderId, OrderStatus currentStatus, OrderStatus newStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId AND o.userId = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(UUID orderId, UUID userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(UUID orderId);
}

package ru.lifevaluable.brewflow.order.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.lifevaluable.brewflow.order.entity.CartItem;
import ru.lifevaluable.brewflow.order.entity.CartItemKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, CartItemKey> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartItem c WHERE c.id.userId = :userId AND c.id.productId = :productId")
    Optional<CartItem> findByIdWithLock(UUID userId, UUID productId);

    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.id.userId = :userId")
    List<CartItem> findByIdUserIdWithProducts(UUID userId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.id.userId = :userId AND ci.id.productId = :productId")
    int removeByIdUserIdAndIdProductId(UUID userId, UUID productId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.id.userId = :userId")
    int removeByIdUserId(UUID userId);
}

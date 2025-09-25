package ru.lifevaluable.brewflow.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.lifevaluable.brewflow.order.entity.CartItem;
import ru.lifevaluable.brewflow.order.entity.CartItemKey;

import java.util.List;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, CartItemKey> {
    List<CartItem> findByIdUserId(UUID userId);

    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.id.userId = :userId")
    List<CartItem> findByIdUserIdWithProducts(UUID userId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.id.userId = :userId AND ci.id.productId = :productId")
    int removeByIdUserIdAndIdProductId(UUID userId, UUID productId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.id.userId = :userId")
    int removeByIdUserId(UUID userId);
}

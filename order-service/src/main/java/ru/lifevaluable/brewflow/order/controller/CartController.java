package ru.lifevaluable.brewflow.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.lifevaluable.brewflow.order.dto.AddToCartRequest;
import ru.lifevaluable.brewflow.order.dto.CartItemResponse;
import ru.lifevaluable.brewflow.order.dto.CartResponse;
import ru.lifevaluable.brewflow.order.service.CartService;

import java.util.UUID;

@Tag(name = "Cart", description = "Управление корзиной покупок")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Validated
public class CartController {
    private final CartService cartService;

    @Operation(
            summary = "Получить корзину пользователя",
            description = "Возвращает содержимое корзины текущего пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Корзина успешно получена"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @GetMapping
    public ResponseEntity<CartResponse> getMyCart(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(cartService.getUserCart(userId));
    }

    @Operation(
            summary = "Добавить товар в корзину",
            description = "Добавляет товар в корзину пользователя или увеличивает количество существующего товара"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Товар успешно добавлен в корзину"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "404", description = "Товар не найден"),
            @ApiResponse(responseCode = "409", description = "Недостаточно товара на складе")
    })
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addItemToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        CartItemResponse response = cartService.addItemToCart(userId, request.productId(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Удалить товар из корзины",
            description = "Полностью удаляет товар из корзины пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Товар успешно удален из корзины"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "404", description = "Товар не найден в корзине")
    })
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItemFromCart(
            Authentication auth,
            @Parameter(description = "Идентификатор товара", required = true)
            @PathVariable("productId") UUID productId) {
        UUID userId = (UUID) auth.getPrincipal();
        cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Очистить корзину",
            description = "Удаляет все товары из корзины пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Корзина успешно очищена"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}

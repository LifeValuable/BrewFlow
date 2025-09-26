package ru.lifevaluable.brewflow.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.lifevaluable.brewflow.order.client.UserServiceClient;
import ru.lifevaluable.brewflow.order.dto.OrderResponse;
import ru.lifevaluable.brewflow.order.dto.OrdersHistoryResponse;
import ru.lifevaluable.brewflow.order.dto.UserData;
import ru.lifevaluable.brewflow.order.service.OrderService;

import java.util.UUID;

@Tag(name = "Orders", description = "Управление заказами")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;
    private final UserServiceClient userServiceClient;

    @Operation(
            summary = "Получить историю заказов",
            description = "Возвращает список всех заказов пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "История заказов успешно получена"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @GetMapping
    public ResponseEntity<OrdersHistoryResponse> getOrdersHistory(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(orderService.getOrdersHistory(userId));
    }

    @Operation(
            summary = "Получить детали заказа",
            description = "Возвращает подробную информацию о конкретном заказе"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Детали заказа успешно получены"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Идентификатор заказа", required = true)
            @PathVariable("orderId") UUID orderId,
            Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(orderService.getOrderDetails(orderId, userId));
    }

    @Operation(
            summary = "Создать заказ из корзины",
            description = "Создает новый заказ на основе товаров в корзине пользователя"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Заказ успешно создан"),
            @ApiResponse(responseCode = "400", description = "Корзина пуста или содержит некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "409", description = "Недостаточно товара на складе"),
            @ApiResponse(responseCode = "503", description = "Сервис пользователей недоступен")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrderFromCart(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        UserData userData = userServiceClient.getUser(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrderFromCart(userId, userData));
    }
}

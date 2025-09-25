package ru.lifevaluable.brewflow.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.lifevaluable.brewflow.order.dto.UpdateOrderStatusRequest;
import ru.lifevaluable.brewflow.order.service.OrderService;

import java.util.UUID;

@Tag(name = "Internal", description = "Внутренние API для взаимодействия между микросервисами")
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
@Validated
public class InternalOrderController {
    private final OrderService orderService;

    @Operation(
            summary = "Обновить статус заказа",
            description = "Обновляет статус заказа (используется другими микросервисами)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус заказа успешно обновлен"),
            @ApiResponse(responseCode = "400", description = "Некорректный переход статуса"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @Parameter(description = "Идентификатор заказа", required = true)
            @PathVariable("orderId") UUID orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        orderService.updateOrderStatus(orderId, request.userId(), request.currentStatus(), request.newStatus());
        return ResponseEntity.ok().build();
    }
}

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
import ru.lifevaluable.brewflow.order.dto.ProductResponse;
import ru.lifevaluable.brewflow.order.service.ProductService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Products", description = "Каталог товаров")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Validated
public class ProductController {
    private final ProductService productService;

    @Operation(
            summary = "Получить каталог товаров",
            description = "Возвращает список всех доступных товаров в каталоге"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Каталог товаров успешно получен")
    })
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getCatalog() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @Operation(
            summary = "Получить информацию о товаре",
            description = "Возвращает детальную информацию о конкретном товаре"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Информация о товаре успешно получена"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @Parameter(description = "Идентификатор товара", required = true)
            @PathVariable("productId") UUID productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }
}
